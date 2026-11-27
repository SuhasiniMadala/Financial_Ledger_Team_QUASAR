<#
.SYNOPSIS
  local-check skill (SRS SK-02): prove the Financial Ledger works on this machine.

.DESCRIPTION
  1. Build the project (mvn package, tests skipped)          - PASS if exit code 0
  2. Run the automated test suite (mvn test)                  - PASS if all tests pass
  3. Start the application with a temporary database          - PASS if healthy within the timeout
  4. Call the read-only endpoint GET /api/v1/health            - PASS if HTTP 200 and status UP
  5. Stop the application and delete the temporary database   - PASS if the process is stopped
  Stops at the first failure and marks later steps NOT RUN (SK-02.1). Never touches persistent data and
  always stops the application it started (SK-02.2). Works in Windows PowerShell 5.1 and PowerShell 7.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File brain/skills/local-check/scripts/local-check.ps1
.EXAMPLE
  pwsh brain/skills/local-check/scripts/local-check.ps1 -Port 18090 -ReportPath target/local-check-report.md
#>
[CmdletBinding()]
param(
    [int]$Port = 18080,
    [int]$StartupTimeoutSeconds = 60,
    [string]$ReportPath = ""
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')).Path
Set-Location $repo
$onWindows = ($env:OS -eq 'Windows_NT')
$mvn = 'mvn'
if ($onWindows) { $mvn = 'mvn.cmd' }

$temp = Join-Path ([IO.Path]::GetTempPath()) ('ledger-local-check-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temp | Out-Null

$names = @('1. Build the project', '2. Run the automated test suite', '3. Start the application (temporary database)',
           '4. GET /api/v1/health (read-only)', '5. Stop the application and clean up')
$status = @{}; $detail = @{}
foreach ($n in $names) { $status[$n] = 'NOT RUN'; $detail[$n] = '' }
$app = $null
$failed = $false

function Get-Tail([string]$file, [int]$lines = 15) {
    if (Test-Path $file) { return ((Get-Content $file -Tail $lines) -join "`n") }
    return ''
}

function Get-RealJava {
    # DEF-005: on Windows 'java' on PATH can be Oracle's javapath launcher stub, which starts the real JVM as a
    # separate process. Stopping the stub would leave the server running, so start the JVM from java.home directly.
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $line = (& java -XshowSettings:properties -version 2>&1 | ForEach-Object { "$_" } | Where-Object { $_ -match 'java\.home\s*=' } |
             Select-Object -First 1)
    $ErrorActionPreference = $previous
    if ($line) {
        $javaHome = ($line -split '=', 2)[1].Trim()
        foreach ($exe in @('java.exe', 'java')) {
            $candidate = Join-Path (Join-Path $javaHome 'bin') $exe
            if (Test-Path $candidate) { return $candidate }
        }
    }
    return 'java'
}

function Stop-ProcessesMentioning([string]$marker) {
    # Safety net for SK-02.2: stop any process whose command line contains this run's unique temp folder name.
    if ($env:OS -eq 'Windows_NT') {
        Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($marker) } |
            ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    } else {
        Get-Process | Where-Object { $_.CommandLine -and $_.CommandLine.Contains($marker) } |
            ForEach-Object { Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue }
    }
}

function Invoke-Maven([string]$arguments, [string]$log) {
    $p = Start-Process -FilePath $mvn -ArgumentList $arguments -NoNewWindow -Wait -PassThru `
        -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    return $p.ExitCode
}

try {
    # ---- Step 1: build ----
    $step = $names[0]
    $log = Join-Path $temp 'build.log'
    $code = Invoke-Maven '-B -DskipTests package' $log
    if ($code -eq 0) { $status[$step] = 'PASS'; $detail[$step] = 'mvn -B -DskipTests package: exit code 0' }
    else { $status[$step] = 'FAIL'; $detail[$step] = "exit code $code`n" + (Get-Tail $log); $failed = $true }

    # ---- Step 2: tests ----
    if (-not $failed) {
        $step = $names[1]
        $log = Join-Path $temp 'test.log'
        $code = Invoke-Maven '-B test' $log
        $summary = (Select-String -Path $log -Pattern 'Tests run: \d+, Failures: \d+, Errors: \d+, Skipped: \d+$' |
                    Select-Object -Last 1).Line
        if ($code -eq 0) { $status[$step] = 'PASS'; $detail[$step] = "mvn -B test: $summary" }
        else { $status[$step] = 'FAIL'; $detail[$step] = "exit code $code; $summary`n" + (Get-Tail $log 25); $failed = $true }
    }

    # ---- Step 3: start the application on a temporary database ----
    if (-not $failed) {
        $step = $names[2]
        $jar = Get-ChildItem -Path (Join-Path $repo 'target') -Filter 'financial-ledger-*.jar' |
               Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
        $busy = $false
        try { $probe = New-Object System.Net.Sockets.TcpClient('127.0.0.1', $Port); $probe.Close(); $busy = $true } catch { $busy = $false }
        if ($null -eq $jar) {
            $status[$step] = 'FAIL'; $detail[$step] = 'No application jar in target/'; $failed = $true
        } elseif ($busy) {
            $status[$step] = 'FAIL'; $detail[$step] = "Port $Port is already in use. Stop the other program or use -Port."; $failed = $true
        } else {
            $db = Join-Path $temp 'local-check.db'
            $appArgs = @('-jar', "`"$($jar.FullName)`"", "--server.port=$Port", "`"--ledger.db.path=$db`"",
                         '--ledger.jwt.allow-ephemeral-secret=true')
            $app = Start-Process -FilePath (Get-RealJava) -ArgumentList $appArgs -NoNewWindow -PassThru `
                -RedirectStandardOutput (Join-Path $temp 'app.log') -RedirectStandardError (Join-Path $temp 'app.err.log')
            $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
            $ready = $false
            $started = Get-Date
            while ((Get-Date) -lt $deadline -and -not $app.HasExited) {
                try {
                    $r = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$Port/api/v1/health" -TimeoutSec 2
                    if ($r.StatusCode -eq 200) { $ready = $true; break }
                } catch { Start-Sleep -Milliseconds 500 }
            }
            $secs = [int]((Get-Date) - $started).TotalSeconds
            if ($ready) { $status[$step] = 'PASS'; $detail[$step] = "Started $($jar.Name) on port $Port in about $secs s (temporary database)" }
            else { $status[$step] = 'FAIL'; $detail[$step] = "Not ready within $StartupTimeoutSeconds s`n" + (Get-Tail (Join-Path $temp 'app.log') 25); $failed = $true }
        }
    }

    # ---- Step 4: read-only fetch ----
    if (-not $failed) {
        $step = $names[3]
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:$Port/api/v1/health" -TimeoutSec 10
            $body = $r.Content | ConvertFrom-Json
            if ($r.StatusCode -eq 200 -and $body.status -eq 'UP') {
                $status[$step] = 'PASS'; $detail[$step] = "HTTP 200 $($r.Content)"
            } else { $status[$step] = 'FAIL'; $detail[$step] = "HTTP $($r.StatusCode) $($r.Content)"; $failed = $true }
        } catch { $status[$step] = 'FAIL'; $detail[$step] = $_.Exception.Message; $failed = $true }
    }
}
finally {
    # ---- Step 5: always stop what we started and remove temporary files (SK-02.2) ----
    $step = $names[4]
    $stopped = $true
    if ($null -ne $app -and -not $app.HasExited) {
        Stop-Process -Id $app.Id -Force
        $stopped = $app.WaitForExit(15000)
    }
    Stop-ProcessesMentioning (Split-Path -Leaf $temp)
    Start-Sleep -Milliseconds 500
    try {
        $probe = New-Object System.Net.Sockets.TcpClient('127.0.0.1', $Port); $probe.Close()
        if ($null -ne $app) { $stopped = $false }   # something we started is still listening
    } catch { }
    try { Remove-Item -Recurse -Force $temp } catch { }
    if (-not $failed) {
        if ($stopped) { $status[$step] = 'PASS'; $detail[$step] = 'Application stopped; temporary database deleted' }
        else { $status[$step] = 'FAIL'; $detail[$step] = 'Application did not stop'; $failed = $true }
    } else {
        $detail[$step] = 'Not run as a step because an earlier step failed; application stopped and temporary files removed anyway'
    }
}

$overall = 'PASS'
if ($failed) { $overall = 'FAIL' }
# DEF-004: 'java -version' writes to stderr; under ErrorActionPreference=Stop, Windows PowerShell 5.1 turns
# that into a terminating error. Native tools are therefore called with 'Continue' here.
$ErrorActionPreference = 'Continue'
$commit = (& git rev-parse --short HEAD 2>$null)
$javaVersion = (& java -version 2>&1 | Select-Object -First 1 | ForEach-Object { "$_" })
$ErrorActionPreference = 'Stop'
$lines = @('# local-check report', '',
    "- Date: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')",
    "- Machine: $([Environment]::OSVersion.VersionString); PowerShell $($PSVersionTable.PSVersion)",
    "- Java: $javaVersion",
    "- Commit: $commit", '',
    '| Step | Result | Details |', '|---|---|---|')
foreach ($n in $names) {
    $d = ($detail[$n] -split "`n")[0] -replace '\|', '/'
    $lines += "| $n | $($status[$n]) | $d |"
}
$lines += ''
$lines += "**Overall: $overall**"
$report = $lines -join "`n"
Write-Output $report
foreach ($n in $names) {
    if ($status[$n] -eq 'FAIL') { Write-Output "`n--- Error output for '$n' ---`n$($detail[$n])" }
}
if ($ReportPath) {
    $dir = Split-Path -Parent $ReportPath
    if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
    Set-Content -Path $ReportPath -Value $report -Encoding UTF8
}
if ($failed) { exit 1 } else { exit 0 }
