# Stage 4 Demonstration Environment

Owner: Geethika M. Used by all three demo guides (`docs/review/demos/`) and the demo script (`docs/review/stage-4-demo-script.md`).

All money is simulated. Use only the synthetic people below (`@example.test` addresses never receive mail). Never type a real password you use elsewhere.

## 1. Prerequisites (check once)

| Tool | Command | Needed |
|---|---|---|
| Java 17+ | `java -version` | backend |
| Maven 3.8+ | `mvn -v` | backend build |
| Node.js 24.15+ (or 22.22+) | `node -v` | React client |
| Git | `git --version` | getting `main` |

## 2. Get the final code

```powershell
cd $HOME\Documents\Financial_Ledger_Team_QUASAR
git switch main
git pull origin main
mvn -B -DskipTests package
cd frontend
npm ci
cd ..
```

## 3. Start the backend on a fresh demo database (terminal 1)

The admin account exists only from these environment variables (FR-AUTH-05). They live only in this terminal and are never written to a file.

```powershell
$env:LEDGER_JWT_SECRET = -join ((48..57 + 65..90 + 97..122) | Get-Random -Count 48 | ForEach-Object { [char]$_ })
$env:LEDGER_ADMIN_EMAIL = 'admin@example.test'
$env:LEDGER_ADMIN_PASSWORD = Read-Host 'Choose a local demo admin password'   # type it, press Enter
$demoDb = Join-Path $env:TEMP 'quasar-demo-ledger.db'
java -jar (Get-Item target/financial-ledger-*.jar).FullName --ledger.db.path=$demoDb
```

Ready when the log shows `Started FinancialLedgerApplication`. Check in a browser: http://localhost:8080/api/v1/health shows `{"database":"UP","status":"UP"}`. Swagger UI: http://localhost:8080/swagger-ui.html.

## 4. Start the React client (terminal 2)

```powershell
cd $HOME\Documents\Financial_Ledger_Team_QUASAR\frontend
npm run dev
```

Open http://localhost:5173. Use a private/incognito window per role if you want USER, MERCHANT and ADMIN open at the same time (each tab keeps its own session).

## 5. Demo people (SRS running example)

| Role | Name | Email | Created by |
|---|---|---|---|
| USER | Asha Rao | `asha@example.test` | Member 1 demo (Register) |
| USER | Ravi Kumar | `ravi@example.test` | Member 1 demo (Register) |
| MERCHANT | Campus Cafe | `cafe@example.test` | Member 1 demo (Register as merchant) |
| ADMIN | Platform Admin | value of `LEDGER_ADMIN_EMAIL` | configuration (step 3) |

Use one demo password for the three registered people (8-72 characters) and keep it only in your head or a local note that is never committed.

Write down during the demo (on paper or a local scratch file): Asha's INR wallet ID, Ravi's INR wallet ID, Campus Cafe's merchant code. Later steps need them.

## 6. Reset to a clean state

1. Stop the backend (terminal 1: `Ctrl+C`).
2. Delete the demo database: `Remove-Item (Join-Path $env:TEMP 'quasar-demo-ledger.db*')` (removes the `-wal` and `-shm` files too).
3. Start again from step 3. All people and money are gone; nothing else on the machine is touched.

## 7. If something fails during the live demo

| Symptom | Fix |
|---|---|
| Client shows "Could not reach the server" | backend stopped: restart step 3 (the database file keeps all data), then press Retry in the client (same key, safe) |
| Port 8080 already in use | `Get-NetTCPConnection -LocalPort 8080 -State Listen \| Select OwningProcess` then stop that process, or add `--server.port=8081` and start the client with `$env:LEDGER_API_URL='http://localhost:8081'; npm run dev` |
| Port 5173 already in use | close the other `npm run dev` terminal |
| "Your session has expired" | tokens last 60 minutes (NFR-SEC-02); log in again |
| Admin login fails | the env vars in terminal 1 were not set before `java -jar`; stop, set them, start again |
| Wrong data from a previous run | reset (section 6) |
| Client completely broken | fall back to Swagger UI at http://localhost:8080/swagger-ui.html for the same requests, and say so in the demo |
