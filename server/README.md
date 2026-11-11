# Single-branch ledger and Android field test

Use Python 3.10 or newer. Set SAPTADHAN_DB to an absolute path on a persistent volume with write access restricted to the ledger service. Its SQLite database holds officer PIN hashes, expiring sessions, transfer confirmations and payments.

1. Run `python server/ledger.py register TEST-OFFICER` on the server. Enter a test PIN at the hidden prompt. Registration and transfer confirmation are operator commands.
2. For an isolated test database, use the app's fictional fixture group JLG-KTH-001. Run `python server/ledger.py confirm-transfer JLG-KTH-001 TEST-TRANSFER-001 YYYY-MM-DD`, substituting a date at least 14 days ago. For live funds, enter the actual transfer date and bank reference only after verifying settlement. This command records confirmation and does not initiate a transfer.
3. Run `python server/ledger.py serve` to start the loopback test server. Route an HTTPS test endpoint with a device-trusted certificate to it. For a deployed service use a managed WSGI host with TLS, persistence, backups and login rate limits.
4. In the Android project's ignored .env file set `LEDGER_API_URL=https://your-ledger-host` to the real service URL. Rebuild/install the debug app with Android Studio, SDK 36.1, JDK 21 and Gradle 9.3.1. The URL is public configuration. Keep officer credentials off the build machine and out of Git.
5. On a phone with a strong biometric enrolled, try a wrong PIN, then sign in online with the registered account and **Remember me** checked. Open **Sync → Refresh Confirmed Transfers**. The existing plan starts installments 14 calendar days after disbursement, with 24 fortnightly installments. An approved group without confirmed funds has no collectible balance.
6. Turn off Wi-Fi and mobile data. Choose a test borrower with at least ₹1,000 due, record ₹1,000 cash, and copy the receipt number. Force-stop the app without clearing storage. Open it again, cancel biometric login once to verify access stays locked, then complete biometric login. Open borrower history and verify the original receipt still shows **Pending**. Offline biometric access expires 12 hours after PIN login.
7. Restore internet and tap **Sync payments** twice. The receipt should show **Synced**. On the test server run the following parameterized query against SAPTADHAN_DB, binding the exact receipt. Expect one row with that receipt and 100000 paise.

```sql
SELECT receipt, COUNT(*), SUM(amount_paise)
FROM payments WHERE receipt = ? GROUP BY receipt;
```

Run `python -m unittest discover -s server -v` for the HTTP replay test. With an Android device attached, run `gradle :app:connectedDebugAndroidTest` for the Room close/reopen test. The existing sample roster and records remain in the app. Older receipts have no verified collecting officer or acknowledgment and are labelled unverified; reconcile them against branch records separately. Use an isolated database and test phone for the pilot.
