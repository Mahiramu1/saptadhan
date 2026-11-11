"""Exercise the HTTP/SQLite boundary with a fictional officer and payment."""
from concurrent.futures import ThreadPoolExecutor
from contextlib import closing
from datetime import datetime, timedelta
import http.client
import json
import os
from pathlib import Path
import sqlite3
import tempfile
import threading
import time
import unittest
from socketserver import ThreadingMixIn
from wsgiref.simple_server import make_server, WSGIServer
import ledger


class ThreadedServer(ThreadingMixIn, WSGIServer):
    daemon_threads = True


class LedgerReplay(unittest.TestCase):
    def test_offline_restart_and_repeat_upload(self):
        with tempfile.TemporaryDirectory() as folder:
            ledger.DATABASE = os.path.join(folder, "server.db")
            with closing(sqlite3.connect(ledger.DATABASE)) as db, db:
                db.executescript(ledger.SCHEMA)
                ledger.register(db, "TEST-OFFICER", "680235")
            with make_server("127.0.0.1", 0, ledger.application, server_class=ThreadedServer) as server:
                thread = threading.Thread(target=server.serve_forever, daemon=True)
                thread.start()
                self.addCleanup(server.shutdown)
                def post(path, body, token=""):
                    conn = http.client.HTTPConnection("127.0.0.1", server.server_port, timeout=10)
                    try:
                        conn.request("POST", path, json.dumps(body), {"Authorization": "Bearer " + token})
                        response = conn.getresponse()
                        return response.status, json.loads(response.read())
                    finally:
                        conn.close()
                for officer in ("TEST-OFFICER", "UNKNOWN"):
                    self.assertEqual(post("/login", {"officer": officer, "pin": "000000"})[0], 401)
                status, session = post("/login", {"officer": "TEST-OFFICER", "pin": "680235"})
                self.assertEqual(status, 200)
                payment = dict(receipt="REC-TEST-1000", officer="TEST-OFFICER", group="JLG-TEST", center="CENTER-TEST",
                               borrower="FICTIONAL", amountPaise=100000, mode="CASH", timestamp=int(time.time()*1000))
                # A file-backed test outbox models a disconnected client. The Android Room test covers the phone database.
                outbox = Path(folder) / "outbox.db"
                with closing(sqlite3.connect(outbox)) as local, local:
                    local.execute("CREATE TABLE queue(payload TEXT,pending INTEGER)")
                    local.execute("INSERT INTO queue VALUES(?,1)", (json.dumps(payment),))
                with closing(sqlite3.connect(outbox)) as local:
                    payload, pending = local.execute("SELECT * FROM queue").fetchone()
                self.assertEqual(pending, 1)
                payment = json.loads(payload)
                token = session["token"]
                self.assertEqual(post("/payments", payment)[0], 401)
                self.assertEqual(post("/payments", payment, token)[0], 409)
                paid_on = (datetime.now(ledger.INDIA).date()-timedelta(days=14)).isoformat()
                with closing(sqlite3.connect(ledger.DATABASE)) as db, db:
                    db.execute("INSERT INTO disbursements VALUES(?,?,?)", ("JLG-TEST", "TEST-TRANSFER", paid_on))
                self.assertEqual(post("/disbursements", {}, token)[1]["groups"]["JLG-TEST"], paid_on)
                with ThreadPoolExecutor(2) as pool:
                    results = list(pool.map(lambda _: post("/payments", payment, token), range(2)))
                self.assertEqual(results, [(200, {"saved": True, "receipt": "REC-TEST-1000"})]*2)
                self.assertEqual(post("/payments", dict(payment, amountPaise=200000), token)[0], 409)
                with closing(sqlite3.connect(ledger.DATABASE)) as db:
                    self.assertEqual(db.execute("SELECT receipt,amount_paise FROM payments").fetchall(),
                                     [("REC-TEST-1000", 100000)])
                server.shutdown()


if __name__ == "__main__":
    unittest.main()
