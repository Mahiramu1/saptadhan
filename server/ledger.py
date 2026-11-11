"""Single-branch SaptaDhan WSGI API. Put behind HTTPS for Android clients."""
import argparse
from contextlib import closing
from datetime import date, datetime, timezone, timedelta
from getpass import getpass
import hashlib
import hmac
import json
import os
import re
import secrets
import sqlite3
import time
from wsgiref.simple_server import make_server

DATABASE = os.environ.get("SAPTADHAN_DB", "saptadhan.sqlite3")
INDIA = timezone(timedelta(hours=5, minutes=30))
SCHEMA = """
CREATE TABLE IF NOT EXISTS officers(code TEXT PRIMARY KEY, salt BLOB NOT NULL,
 digest BLOB NOT NULL, failures INTEGER DEFAULT 0, blocked_until INTEGER DEFAULT 0);
CREATE TABLE IF NOT EXISTS sessions(digest TEXT PRIMARY KEY, officer TEXT NOT NULL, expires INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS disbursements(group_id TEXT PRIMARY KEY, transfer_ref TEXT UNIQUE NOT NULL, paid_on TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS payments(receipt TEXT PRIMARY KEY, officer TEXT NOT NULL, payload TEXT NOT NULL, amount_paise INTEGER NOT NULL);
"""


class Failure(Exception):
    def __init__(self, status, message):
        super().__init__(message)
        self.status = status


def pin_hash(pin, salt):
    return hashlib.pbkdf2_hmac("sha256", pin.encode(), salt, 600000)


def register(db, code, pin):
    if not code.strip() or not re.fullmatch(r"[0-9]{6,12}", pin):
        raise ValueError("Enter an officer code and a 6 to 12 digit PIN")
    salt = secrets.token_bytes(16)
    db.execute("INSERT INTO officers(code,salt,digest) VALUES(?,?,?)", (code, salt, pin_hash(pin, salt)))


def login(db, body):
    code, pin = body.get("officer"), body.get("pin")
    if not isinstance(code, str) or not isinstance(pin, str) or len(pin) > 128:
        raise Failure("401 Unauthorized", "Invalid credentials")
    db.execute("BEGIN IMMEDIATE")
    row = db.execute("SELECT salt,digest,failures,blocked_until FROM officers WHERE code=?", (code,)).fetchone()
    now = int(time.time())
    if row and row[3] > now:
        raise Failure("429 Too Many Requests", "Try again later")
    candidate = pin_hash(pin, row[0] if row else bytes(16))
    if not row or not hmac.compare_digest(candidate, row[1]):
        if row:
            attempts = row[2] + 1 if not row[3] else 1
            db.execute("UPDATE officers SET failures=?,blocked_until=? WHERE code=?",
                       (attempts, now + 300 if attempts >= 5 else 0, code))
            db.commit()
        raise Failure("401 Unauthorized", "Invalid credentials")
    token = secrets.token_urlsafe(32)
    expires = now + 12 * 3600
    db.execute("UPDATE officers SET failures=0,blocked_until=0 WHERE code=?", (code,))
    db.execute("INSERT INTO sessions VALUES(?,?,?)", (token_hash(token), code, expires))
    return {"officer": code, "token": token, "expires": expires}


def token_hash(token):
    return hashlib.sha256(token.encode()).hexdigest()


def dispatch(db, path, body, auth):
    if path == "/login":
        return login(db, body)
    session = db.execute("SELECT officer FROM sessions WHERE digest=? AND expires>?",
                         (token_hash(auth.removeprefix("Bearer ")), int(time.time()))).fetchone()
    if not auth.startswith("Bearer ") or not session:
        raise Failure("401 Unauthorized", "Sign in again")
    if path == "/disbursements":
        rows = db.execute("SELECT group_id,paid_on FROM disbursements ORDER BY group_id")
        return {"groups": dict(rows)}
    if path != "/payments":
        raise Failure("404 Not Found", "Unknown endpoint")
    for field in ("receipt", "borrower", "group", "center", "mode"):
        if not isinstance(body.get(field), str) or not 0 < len(body[field]) <= 160:
            raise Failure("400 Bad Request", "Invalid " + field)
    amount, stamp = body.get("amountPaise"), body.get("timestamp")
    if type(amount) is not int or not 0 <= amount <= 10**10 or type(stamp) is not int or not 0 < stamp <= (time.time()+300)*1000:
        raise Failure("400 Bad Request", "Invalid amount or collection time")
    if body["mode"] not in {"CASH", "MUTUAL_GUARANTEE"} or (amount == 0 and not body.get("overdueReason")):
        raise Failure("400 Bad Request", "Unsupported or unverified payment")
    if body.get("officer") != session[0]:
        raise Failure("403 Forbidden", "Receipt belongs to another officer")
    db.execute("BEGIN IMMEDIATE")
    paid = db.execute("SELECT paid_on FROM disbursements WHERE group_id=?", (body["group"],)).fetchone()
    collected = datetime.fromtimestamp(stamp/1000, INDIA).date()
    if not paid or collected < date.fromisoformat(paid[0]) + timedelta(days=14):
        raise Failure("409 Conflict", "No installment due on this collection date")
    serialized = json.dumps(body, sort_keys=True, separators=(",", ":"))
    db.execute("INSERT INTO payments VALUES(?,?,?,?) ON CONFLICT(receipt) DO NOTHING",
               (body["receipt"], session[0], serialized, amount))
    saved = db.execute("SELECT payload FROM payments WHERE receipt=?", (body["receipt"],)).fetchone()[0]
    if saved != serialized:
        raise Failure("409 Conflict", "Receipt already has different payment details")
    return {"saved": True, "receipt": body["receipt"]}


def application(env, respond):
    try:
        if env["REQUEST_METHOD"] != "POST":
            raise Failure("405 Method Not Allowed", "Use POST")
        size = int(env.get("CONTENT_LENGTH") or 0)
        if not 0 < size <= 16384:
            raise Failure("400 Bad Request", "Invalid body size")
        body = json.loads(env["wsgi.input"].read(size))
        if not isinstance(body, dict):
            raise Failure("400 Bad Request", "Expected JSON object")
        with closing(sqlite3.connect(DATABASE, timeout=15)) as db, db:
            result = dispatch(db, env["PATH_INFO"], body, env.get("HTTP_AUTHORIZATION", ""))
        status = "200 OK"
    except Failure as error:
        status, result = error.status, {"error": str(error)}
    except (ValueError, UnicodeError):
        status, result = "400 Bad Request", {"error": "Invalid input"}
    respond(status, [("Content-Type", "application/json"), ("Cache-Control", "no-store")])
    return [json.dumps(result).encode()]


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("register").add_argument("officer")
    disburse = commands.add_parser("confirm-transfer")
    disburse.add_argument("group")
    disburse.add_argument("reference")
    disburse.add_argument("paid_on", type=date.fromisoformat)
    serve = commands.add_parser("serve")
    serve.add_argument("--port", type=int, default=8080)
    args = parser.parse_args()
    with closing(sqlite3.connect(DATABASE)) as db, db:
        db.executescript(SCHEMA)
        if args.command == "register":
            register(db, args.officer, getpass("Officer PIN: "))
        elif args.command == "confirm-transfer":
            if args.paid_on > datetime.now(INDIA).date() or not args.reference.strip():
                parser.error("A completed transfer reference and actual payment date are required")
            db.execute("INSERT INTO disbursements VALUES(?,?,?)", (args.group, args.reference, args.paid_on.isoformat()))
    if args.command == "serve":
        with make_server("127.0.0.1", args.port, application) as httpd:
            httpd.serve_forever()
