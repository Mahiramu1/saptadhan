#!/usr/bin/env bash
set -euo pipefail
app=com.aistudio.disrubcollect.wnptg
runner="$app.test/androidx.test.runner.AndroidJUnitRunner"
state=$(mktemp -d)
export SAPTADHAN_DB="$state/test.sqlite3"
export TEST_PIN=$(python3 -c 'import secrets; print(secrets.randbelow(900000)+100000)')
export PYTHONPATH=server
python3 - <<'PY' &
from datetime import datetime, timedelta
from wsgiref.simple_server import make_server
import ssl, sqlite3, os, ledger
with sqlite3.connect(ledger.DATABASE) as db:
    db.executescript(ledger.SCHEMA)
    ledger.register(db, "FIELD-TEST", os.environ["TEST_PIN"])
    paid_on = (datetime.now(ledger.INDIA).date() - timedelta(days=14)).isoformat()
    db.execute("INSERT INTO disbursements VALUES(?,?,?)", ("TEST-JLG","FICTIONAL-TRANSFER",paid_on))
http = make_server("127.0.0.1", 8443, ledger.application)
ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
ctx.load_cert_chain("/tmp/field.crt", "/tmp/field.key")
http.socket = ctx.wrap_socket(http.socket, server_side=True)
http.serve_forever()
PY
server=$!
trap 'kill "$server" || true' EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb reverse tcp:8443 tcp:8443
phase() {
  adb shell am instrument -w -e class com.example.FieldReplayTest -e phase "$1" -e pin "$TEST_PIN" "$runner" > "$state/$1.log"
  cat "$state/$1.log"
  grep -q 'OK (1 test)' "$state/$1.log"
}
phase prepare &
prepare=$!
for _ in $(seq 1 120); do
  if adb shell run-as "$app" test -f files/ready-for-offline; then break; fi
  sleep 1
done
adb reverse --remove tcp:8443
adb shell cmd connectivity airplane-mode enable
adb shell svc wifi disable
adb shell run-as "$app" touch files/offline
wait "$prepare"
adb shell am force-stop "$app"
phase reopen
adb shell cmd connectivity airplane-mode disable
adb shell svc wifi enable
adb reverse tcp:8443 tcp:8443
phase sync
receipt=$(adb shell run-as "$app" cat files/verified-receipt | tr -d '\r')
python3 - "$receipt" <<'PY'
import os, sqlite3, sys
with sqlite3.connect(os.environ["SAPTADHAN_DB"]) as db:
    rows = db.execute("SELECT receipt,amount_paise FROM payments").fetchall()
assert rows == [(sys.argv[1], 100000)], rows
print(f"Verified one receipt {sys.argv[1]} for INR 1000.00")
PY
