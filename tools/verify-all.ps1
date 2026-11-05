# Single delivery gate: governance check, unit tests, APK build, whitespace check.
# Any failure exits non-zero. Run it before closing a task, not instead of the individual tools.
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $repoRoot 'AGENTS.md'))) { $repoRoot = (Get-Location).Path }
Set-Location $repoRoot

$logDir = Join-Path $repoRoot '.gradle\logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

# Gradle needs the local toolchain; only source it when the environment is not already set.
if (-not $env:ANDROID_HOME) {
    $envScript = 'D:\AndroidToolchain\env.ps1'
    if (Test-Path $envScript) { . $envScript | Out-Null }
}

$results = @()
function Invoke-Step([string]$Name, [scriptblock]$Body) {
    Write-Host ''
    Write-Host ('--- ' + $Name + ' ---') -ForegroundColor Cyan
    $started = Get-Date
    $global:LASTEXITCODE = 0
    & $Body
    $exit = $LASTEXITCODE
    $seconds = [int]((Get-Date) - $started).TotalSeconds
    $script:results += [pscustomobject]@{ Step = $Name; Exit = $exit; Seconds = $seconds }
    return $exit
}

$governance = Invoke-Step 'check-governance.ps1' {
    pwsh -NoProfile -File (Join-Path $repoRoot 'tools/check-governance.ps1') 2>&1 |
        Tee-Object -FilePath (Join-Path $logDir 'verify-governance.log') |
        Select-String -Pattern 'SUMMARY|RESULT|FAIL' | ForEach-Object { Write-Host $_.Line }
}

$tests = Invoke-Step 'run-unit-tests.ps1' {
    pwsh -NoProfile -File (Join-Path $repoRoot 'tools/run-unit-tests.ps1') 2>&1 |
        Tee-Object -FilePath (Join-Path $logDir 'verify-tests.log') |
        Select-String -Pattern 'OK \(|Tests run|RESULT:' | ForEach-Object { Write-Host $_.Line }
}

$dao = Invoke-Step 'verify-dao.mjs' {
    node (Join-Path $repoRoot 'tools/verify-dao.mjs') 2>&1 |
        Tee-Object -FilePath (Join-Path $logDir 'verify-dao.log') |
        Select-String -Pattern 'FAIL|checks:|RESULT:' | ForEach-Object { Write-Host $_.Line }
}

$build = Invoke-Step 'assembleDebug' {
    $gradle = if (Test-Path (Join-Path $repoRoot 'gradlew.bat')) { Join-Path $repoRoot 'gradlew.bat' } else { Join-Path $repoRoot 'gradlew' }
    & $gradle assembleDebug --console=plain --no-daemon *> (Join-Path $logDir 'verify-build.log')
    Get-Content (Join-Path $logDir 'verify-build.log') |
        Select-String -Pattern 'BUILD SUCCESSFUL|BUILD FAILED|^e: ' |
        Select-Object -First 6 | ForEach-Object { Write-Host $_.Line }
}

$whitespace = Invoke-Step 'git diff --check' {
    $report = git diff --check
    $report | Tee-Object -FilePath (Join-Path $logDir 'verify-whitespace.log') | ForEach-Object { Write-Host $_ }
    if ($LASTEXITCODE -eq 0) { Write-Host 'no whitespace errors' }
}

Write-Host ''
Write-Host '=== verify-all summary ==='
$results | ForEach-Object { Write-Host ('  ' + $_.Step.PadRight(24) + ' exit ' + $_.Exit + '   ' + $_.Seconds + 's') }
$dirty = @(git status --short)
if ($dirty.Count -gt 0) { Write-Host ('  uncommitted changes: ' + $dirty.Count + ' file(s)') }

$failed = @($results | Where-Object { $_.Exit -ne 0 })
if ($failed.Count -eq 0) {
    Write-Host 'RESULT: VERIFY ALL PASS'
    exit 0
}
Write-Host ('RESULT: VERIFY ALL FAILED (' + ($failed.Step -join ', ') + ')') -ForegroundColor Red
exit 1
