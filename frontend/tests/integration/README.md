# Client integration tests (real backend, no mocks)

`client-contract.int.test.ts` drives the real backend through the client's own API modules: registration and login, test funding with an idempotent retry, a transfer and its reversal, an insufficient-funds rejection, a merchant payment, and a final reconciliation. Every money step checks stored balances, not only the HTTP status, and the run must end with reconciliation PASS.

## Run locally (PowerShell, from the repository root)

```powershell
mvn -B -DskipTests package
$env:LEDGER_ADMIN_EMAIL = 'admin@local.test'
$env:LEDGER_ADMIN_PASSWORD = 'local-only-admin-password'
# Fresh temporary database, random per-process JWT key; nothing is written to the normal database.
$db = Join-Path $env:TEMP "ledger-int-$(Get-Random).db"
$server = Start-Process java -PassThru -ArgumentList '-jar', (Resolve-Path target/financial-ledger-*.jar), "--ledger.db.path=$db", '--ledger.jwt.allow-ephemeral-secret=true'
# wait until http://localhost:8080/api/v1/health answers, then:
cd frontend
npm run test:integration
Stop-Process -Id $server.Id
```

Without `LEDGER_ADMIN_EMAIL` and `LEDGER_ADMIN_PASSWORD` the suite is skipped (it is never reported as passed without running). CI runs it in the job "Frontend integration (real backend)".
