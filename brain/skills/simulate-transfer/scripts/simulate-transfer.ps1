<#
.SYNOPSIS
  simulate-transfer skill (SRS SK-01): demonstrate and verify transfers on a LOCAL test instance.

.DESCRIPTION
  Steps (SRS 6.3.1):
   1. Create two synthetic test wallets A and B and fund A by admin test funding.
   2. Record the initial balances.
   3. Transfer A -> B with a new Idempotency-Key, then resend it identically (must replay, one transaction).
   4. Fire simultaneous transfers from A (distinct keys) whose total exceeds A's balance.
   5. Call the reconciliation endpoint (must PASS with 0 discrepancies).
   6. Summarise initial, final and expected balances; overall PASS only if steps 1-5 passed.
  With -Evaluate it also runs the TQ-7 evaluation cases and reports PASS/FAIL per case.
  Uses only the public API (SK-01.1). Admin credentials come from environment variables (SK-01.2).
  Refuses to run against anything except localhost. All accounts are synthetic.

.EXAMPLE
  $env:LEDGER_ADMIN_EMAIL = 'admin@local.test'; $env:LEDGER_ADMIN_PASSWORD = '<the value you started the server with>'
  powershell -ExecutionPolicy Bypass -File brain/skills/simulate-transfer/scripts/simulate-transfer.ps1 -Evaluate
#>
[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [long]$AmountMinor = 10000,
    [int]$Concurrency = 10,
    [switch]$Evaluate,
    [string]$ReportPath = ''
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

# ---------- safety checks ----------
function Exit-Refused([string]$message) {
    [Console]::Error.WriteLine("REFUSED: $message")
    exit 2
}
$uri = [Uri]$BaseUrl
if (@('localhost', '127.0.0.1', '::1', '[::1]') -notcontains $uri.Host) {
    Exit-Refused "will not run against '$($uri.Host)'. This skill only runs against a local test instance."
}
if (-not $env:LEDGER_ADMIN_EMAIL -or -not $env:LEDGER_ADMIN_PASSWORD) {
    Exit-Refused 'set LEDGER_ADMIN_EMAIL and LEDGER_ADMIN_PASSWORD (the admin the local server was started with).'
}
if ($AmountMinor -lt 1 -or $Concurrency -lt 2) {
    Exit-Refused 'AmountMinor must be >= 1 and Concurrency >= 2.'
}

$http = New-Object System.Net.Http.HttpClient
$http.Timeout = [TimeSpan]::FromSeconds(60)
$runId = [guid]::NewGuid().ToString('N').Substring(0, 8)

function New-Key([string]$prefix) { return "$prefix-$([guid]::NewGuid().ToString('N'))" }

function New-ApiRequest([string]$method, [string]$path, $body, [string]$token, [string]$key) {
    $req = New-Object System.Net.Http.HttpRequestMessage((New-Object System.Net.Http.HttpMethod($method)), "$BaseUrl$path")
    if ($token) { $req.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $token) }
    if ($key) { [void]$req.Headers.TryAddWithoutValidation('Idempotency-Key', $key) }
    if ($null -ne $body) {
        if ($body -is [string]) { $json = $body } else { $json = $body | ConvertTo-Json -Compress -Depth 5 }
        $req.Content = New-Object System.Net.Http.StringContent($json, [Text.Encoding]::UTF8, 'application/json')
    }
    return $req
}

function Read-ApiResponse($response) {
    $text = $response.Content.ReadAsStringAsync().Result
    $obj = $null
    if ($text -and ($text.StartsWith('{') -or $text.StartsWith('['))) { $obj = $text | ConvertFrom-Json }
    return [pscustomobject]@{ Status = [int]$response.StatusCode; Body = $obj; Raw = $text
                              Replayed = $response.Headers.Contains('Idempotent-Replayed') }
}

function Invoke-Api([string]$method, [string]$path, $body = $null, [string]$token = $null, [string]$key = $null) {
    return Read-ApiResponse ($http.SendAsync((New-ApiRequest $method $path $body $token $key)).Result)
}

function New-TestUser([string]$label) {
    $email = "sim-$label-$runId-$([guid]::NewGuid().ToString('N').Substring(0, 6))@simulate.test"
    $password = 'Sim-' + [guid]::NewGuid().ToString('N')
    $r = Invoke-Api 'POST' '/api/v1/auth/register' @{ displayName = "Simulated $label"; email = $email; password = $password }
    if ($r.Status -ne 201) { throw "register $label failed: $($r.Status) $($r.Raw)" }
    $l = Invoke-Api 'POST' '/api/v1/auth/login' @{ email = $email; password = $password }
    $w = Invoke-Api 'POST' '/api/v1/wallets' @{ currency = 'INR' } $l.Body.accessToken
    if ($w.Status -ne 201) { throw "create wallet $label failed: $($w.Status) $($w.Raw)" }
    return [pscustomobject]@{ Token = $l.Body.accessToken; Wallet = $w.Body.walletId; Status = $w.Body.status }
}

function Get-Balance($user) {
    $r = Invoke-Api 'GET' "/api/v1/wallets/$($user.Wallet)" $null $user.Token
    return [long]$r.Body.balanceMinor
}

function Send-Transfer($user, [string]$to, $amount, [string]$key) {
    return Invoke-Api 'POST' '/api/v1/transfers' @{ sourceWalletId = $user.Wallet; destinationWalletId = $to; amountMinor = $amount } $user.Token $key
}

$steps = [ordered]@{}
$notes = [ordered]@{}
$failed = $false
function Set-Step([string]$name, [bool]$ok, [string]$note) {
    $script:steps[$name] = $(if ($ok) { 'PASS' } else { 'FAIL' })
    $script:notes[$name] = $note
    if (-not $ok) { $script:failed = $true }
}

# ---------- admin login ----------
$adminLogin = Invoke-Api 'POST' '/api/v1/auth/login' @{ email = $env:LEDGER_ADMIN_EMAIL; password = $env:LEDGER_ADMIN_PASSWORD }
if ($adminLogin.Status -ne 200) { Write-Error "Admin login failed: HTTP $($adminLogin.Status). Is the server running with these credentials?"; exit 1 }
$admin = $adminLogin.Body.accessToken

# ---------- Step 1: two wallets, fund A ----------
$fundAmount = $AmountMinor * $Concurrency
$A = New-TestUser 'A'
$B = New-TestUser 'B'
$fund = Invoke-Api 'POST' "/api/v1/admin/wallets/$($A.Wallet)/test-funding" @{ amountMinor = $fundAmount } $admin (New-Key 'sim-fund')
Set-Step '1. Create wallets A and B, fund A' ($A.Status -eq 'ACTIVE' -and $B.Status -eq 'ACTIVE' -and $fund.Status -eq 201) `
    "A=$($A.Wallet) B=$($B.Wallet) ACTIVE; test funding $fundAmount -> HTTP $($fund.Status)"

# ---------- Step 2: initial balances ----------
$a0 = Get-Balance $A; $b0 = Get-Balance $B
Set-Step '2. Record initial balances' ($a0 -eq $fundAmount -and $b0 -eq 0) "A=$a0 B=$b0"

# ---------- Step 3: transfer + identical resend ----------
$key = New-Key 'sim-transfer'
$t1 = Send-Transfer $A $B.Wallet $AmountMinor $key
$t2 = Send-Transfer $A $B.Wallet $AmountMinor $key
$list = Invoke-Api 'GET' "/api/v1/transactions?walletId=$($A.Wallet)&type=TRANSFER" $null $A.Token
$okStep3 = $t1.Status -eq 201 -and $t2.Status -eq 201 -and $t2.Replayed -and ($t1.Body.transactionId -eq $t2.Body.transactionId) -and ($list.Body.totalItems -eq 1)
Set-Step '3. Transfer, then resend identically' $okStep3 "first $($t1.Status) $($t1.Body.transactionId); resend $($t2.Status) replayed=$($t2.Replayed); transactions on A: $($list.Body.totalItems)"
$a1 = Get-Balance $A; $b1 = Get-Balance $B

# ---------- Step 4: simultaneous transfers exceeding the balance ----------
$tasks = @()
for ($i = 0; $i -lt $Concurrency; $i++) {
    $req = New-ApiRequest 'POST' '/api/v1/transfers' @{ sourceWalletId = $A.Wallet; destinationWalletId = $B.Wallet; amountMinor = $AmountMinor } $A.Token (New-Key "sim-race$i")
    $tasks += $http.SendAsync($req)
}
[System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$tasks)
$results = @($tasks | ForEach-Object { Read-ApiResponse $_.Result })
$ok201 = @($results | Where-Object { $_.Status -eq 201 }).Count
$ok422 = @($results | Where-Object { $_.Status -eq 422 }).Count
$ok503 = @($results | Where-Object { $_.Status -eq 503 }).Count
$other = $Concurrency - $ok201 - $ok422 - $ok503
$a2 = Get-Balance $A; $b2 = Get-Balance $B
$expectA = $a1 - $ok201 * $AmountMinor; $expectB = $b1 + $ok201 * $AmountMinor
Set-Step "4. $Concurrency simultaneous transfers (total $($AmountMinor * $Concurrency) > balance $a1)" `
    ($other -eq 0 -and $a2 -ge 0 -and $a2 -eq $expectA -and $b2 -eq $expectB) "201=$ok201 422=$ok422 503=$ok503 other=$other; A=$a2 (expected $expectA) B=$b2 (expected $expectB)"

# ---------- Step 5: reconciliation ----------
$recon = Invoke-Api 'POST' '/api/v1/admin/reconciliations' $null $admin
$discrepancies = @($recon.Body.discrepancies).Count
Set-Step '5. Reconciliation' ($recon.Status -eq 200 -and $recon.Body.result -eq 'PASS' -and $discrepancies -eq 0) "HTTP $($recon.Status) result=$($recon.Body.result) discrepancies=$discrepancies"

# ---------- Evaluations (TQ-7) ----------
$evals = @()
# DEF-006: the parameter must not be called $input - that is a reserved PowerShell automatic variable and was silently empty.
function Add-Eval([string]$id, [string]$scenario, [string]$inputText, [string]$expected, [string]$actual, [bool]$pass) {
    $script:evals += [pscustomobject]@{ Id = $id; Scenario = $scenario; Input = $inputText; Expected = $expected; Actual = $actual
                                        Result = $(if ($pass) { 'PASS' } else { 'FAIL' }) }
}
if ($Evaluate) {
    $E = New-TestUser 'evalA'; $F = New-TestUser 'evalB'
    [void](Invoke-Api 'POST' "/api/v1/admin/wallets/$($E.Wallet)/test-funding" @{ amountMinor = 50000 } $admin (New-Key 'eval-fund'))
    $limit = 10000000

    $r = Send-Transfer $E $F.Wallet 10000 (New-Key 'e01'); $eb = Get-Balance $E; $fb = Get-Balance $F
    Add-Eval 'E01' 'Valid transfer' '10000 paise E->F' '201; E=40000, F=10000' "$($r.Status); E=$eb, F=$fb" ($r.Status -eq 201 -and $eb -eq 40000 -and $fb -eq 10000)

    $r = Send-Transfer $E $F.Wallet 999999 (New-Key 'e02'); $eb = Get-Balance $E
    Add-Eval 'E02' 'Insufficient balance' '999999 paise' '422 INSUFFICIENT_FUNDS, FAILED txn id, balance unchanged' "$($r.Status) $($r.Body.code) txn=$($r.Body.transactionId); E=$eb" ($r.Status -eq 422 -and $r.Body.code -eq 'INSUFFICIENT_FUNDS' -and $r.Body.transactionId -and $eb -eq 40000)

    $r = Invoke-Api 'POST' '/api/v1/transfers' ('{"sourceWalletId":"' + $E.Wallet + '","destinationWalletId":"' + $F.Wallet + '","amountMinor":100.5}') $E.Token (New-Key 'e03')
    Add-Eval 'E03' 'Invalid amount (fraction)' 'amountMinor 100.5' '400 VALIDATION_ERROR' "$($r.Status) $($r.Body.code)" ($r.Status -eq 400 -and $r.Body.code -eq 'VALIDATION_ERROR')

    $r = Send-Transfer $E $F.Wallet 0 (New-Key 'e04')
    Add-Eval 'E04' 'Zero amount' 'amountMinor 0' '400 VALIDATION_ERROR' "$($r.Status) $($r.Body.code)" ($r.Status -eq 400)

    $r = Send-Transfer $E $F.Wallet -500 (New-Key 'e05')
    Add-Eval 'E05' 'Negative amount' 'amountMinor -500' '400 VALIDATION_ERROR' "$($r.Status) $($r.Body.code)" ($r.Status -eq 400)

    $r = Invoke-Api 'POST' '/api/v1/transfers' @{ sourceWalletId = 'W-0000000000'; destinationWalletId = $F.Wallet; amountMinor = 100 } $E.Token (New-Key 'e06')
    Add-Eval 'E06' 'Nonexistent source account' 'source W-0000000000' '404 NOT_FOUND, nothing recorded' "$($r.Status) $($r.Body.code)" ($r.Status -eq 404)

    $r = Send-Transfer $E 'W-0000000000' 100 (New-Key 'e07')
    Add-Eval 'E07' 'Nonexistent destination account' 'destination W-0000000000' '422 INVALID_DESTINATION' "$($r.Status) $($r.Body.code)" ($r.Status -eq 422 -and $r.Body.code -eq 'INVALID_DESTINATION')

    $r = Send-Transfer $E $E.Wallet 100 (New-Key 'e08')
    Add-Eval 'E08' 'Same-account transfer (BR-06)' 'E->E' '422 SAME_WALLET' "$($r.Status) $($r.Body.code)" ($r.Status -eq 422 -and $r.Body.code -eq 'SAME_WALLET')

    $k = New-Key 'e09'; $r1 = Send-Transfer $E $F.Wallet 100 $k; $r2 = Send-Transfer $E $F.Wallet 100 $k; $eb = Get-Balance $E
    Add-Eval 'E09' 'Duplicate request (same key, same body)' 'same key twice' 'same transactionId, Idempotent-Replayed, charged once' "$($r1.Status)/$($r2.Status) same=$($r1.Body.transactionId -eq $r2.Body.transactionId) replayed=$($r2.Replayed) E=$eb" ($r2.Replayed -and $r1.Body.transactionId -eq $r2.Body.transactionId -and $eb -eq 39900)

    $r = Send-Transfer $E $F.Wallet 200 $k
    Add-Eval 'E10' 'Same key, different payload' 'reuse E09 key with 200' '409 IDEMPOTENCY_KEY_REUSED' "$($r.Status) $($r.Body.code)" ($r.Status -eq 409 -and $r.Body.code -eq 'IDEMPOTENCY_KEY_REUSED')

    $r1 = Send-Transfer $E $F.Wallet 1 (New-Key 'e11a'); $r2 = Send-Transfer $E $F.Wallet ($limit + 1) (New-Key 'e11b')
    Add-Eval 'E11' 'Boundary values' '1 paisa; limit+1' '201; 400' "$($r1.Status); $($r2.Status)" ($r1.Status -eq 201 -and $r2.Status -eq 400)

    $r = Send-Transfer $E $F.Wallet 100 $null
    Add-Eval 'E12' 'Missing Idempotency-Key' 'no header' '400 IDEMPOTENCY_KEY_REQUIRED' "$($r.Status) $($r.Body.code)" ($r.Status -eq 400 -and $r.Body.code -eq 'IDEMPOTENCY_KEY_REQUIRED')

    [void](Invoke-Api 'POST' "/api/v1/admin/wallets/$($F.Wallet)/freeze" @{ reason = 'simulate-transfer evaluation' } $admin)
    $r = Send-Transfer $E $F.Wallet 100 (New-Key 'e13')
    Add-Eval 'E13' 'Destination wallet FROZEN (BR-07)' 'freeze F, transfer' '422 WALLET_NOT_ACTIVE' "$($r.Status) $($r.Body.code)" ($r.Status -eq 422 -and $r.Body.code -eq 'WALLET_NOT_ACTIVE')

    $eb = Get-Balance $E
    $recon2 = Invoke-Api 'POST' '/api/v1/admin/reconciliations' $null $admin
    Add-Eval 'E14' 'Balances consistent after all cases' 'final balances + reconciliation' "E=39899, reconciliation PASS" "E=$eb, $($recon2.Body.result)" ($eb -eq 39899 -and $recon2.Body.result -eq 'PASS')
}

# ---------- Step 6: summary ----------
$ok6 = -not $failed
$steps['6. Summary'] = $(if ($ok6) { 'PASS' } else { 'FAIL' })
$notes['6. Summary'] = 'Overall PASS only if steps 1-5 passed'
$lines = @('# simulate-transfer report', '', "- Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')", "- Base URL: $BaseUrl",
    "- Amount: $AmountMinor paise; concurrency: $Concurrency; run id: $runId", '',
    '| Step | Result | Details |', '|---|---|---|')
foreach ($s in $steps.Keys) { $lines += "| $s | $($steps[$s]) | $($notes[$s]) |" }
$lines += ''
$lines += '| Wallet | Initial | After step 3 | Final | Expected final |'
$lines += '|---|---|---|---|---|'
$lines += "| A $($A.Wallet) | $a0 | $a1 | $a2 | $expectA |"
$lines += "| B $($B.Wallet) | $b0 | $b1 | $b2 | $expectB |"
$lines += ''
$lines += "Reconciliation: $($recon.Body.result) ($discrepancies discrepancies)"
$evalFailed = $false
if ($Evaluate) {
    $lines += ''
    $lines += '## Evaluation cases (TQ-7)'
    $lines += ''
    $lines += '| ID | Scenario | Input | Expected | Actual | Result |'
    $lines += '|---|---|---|---|---|---|'
    foreach ($e in $evals) {
        $lines += "| $($e.Id) | $($e.Scenario) | $($e.Input) | $($e.Expected) | $($e.Actual) | $($e.Result) |"
        if ($e.Result -ne 'PASS') { $evalFailed = $true }
    }
    $lines += "| E15 | Database failure mid-transfer (atomicity) | - | covered by automated test TransferRollbackTest | not injectable against a running server | NOT RUN |"
}
$overall = 'PASS'
if ($failed -or $evalFailed) { $overall = 'FAIL' }
$lines += ''
$lines += "**Overall: $overall**"
$report = $lines -join "`n"
Write-Output $report
if ($ReportPath) {
    $dir = Split-Path -Parent $ReportPath
    if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
    Set-Content -Path $ReportPath -Value $report -Encoding UTF8
}
if ($overall -eq 'PASS') { exit 0 } else { exit 1 }
