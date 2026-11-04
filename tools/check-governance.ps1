# Governance consistency checker for the Habit Tracker repository.
# Read-only. Never modifies files. Exit code 1 if any FAIL is found.
# Usage: pwsh -NoProfile -File tools/check-governance.ps1

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$results = New-Object System.Collections.ArrayList
function Add-Check([string]$name, [string]$status, [string]$detail) {
    [void]$results.Add([pscustomobject]@{ Name = $name; Status = $status; Detail = $detail })
}
function Add-Pass([string]$n, [string]$d) { Add-Check $n 'PASS' $d }
function Add-Fail([string]$n, [string]$d) { Add-Check $n 'FAIL' $d }
function Add-Warn([string]$n, [string]$d) { Add-Check $n 'WARN' $d }

$tracked = @(git ls-files)
if ($LASTEXITCODE -ne 0 -or $tracked.Count -eq 0) {
    Write-Host 'FATAL: not a git repository or no tracked files.' -ForegroundColor Red
    exit 2
}

# 1. Required governance files exist
$required = @(
    'AGENTS.md','README.md','ARCHITECTURE.md','DATABASE.md','DEVELOPMENT.md','.gitignore',
    'project/PROJECT_STATUS.md','project/FEATURE_MATRIX.md','project/ROADMAP.md','project/REQUIREMENTS.md',
    'project/tasks/TASK_TEMPLATE.md','project/handoffs/LAST_HANDOFF.md',
    'project/reports/TEST_REPORT.md','project/reports/BASELINE_REVIEW.md','project/reports/RELEASE_REPORT.md',
    'project/decisions/ADR-TEMPLATE.md'
)
$missing = @($required | Where-Object { -not (Test-Path $_) })
if ($missing.Count -eq 0) { Add-Pass 'required files' ("{0} files present" -f $required.Count) }
else { Add-Fail 'required files' ("missing: " + ($missing -join ', ')) }

# 2. No unresolved UNKNOWN in the status snapshot
$statusText = Get-Content -Raw -Path 'project/PROJECT_STATUS.md'
$unknownCount = ([regex]::Matches($statusText, 'UNKNOWN')).Count
if ($unknownCount -eq 0) { Add-Pass 'status has no UNKNOWN' 'all fields resolved' }
else { Add-Fail 'status has no UNKNOWN' ("{0} occurrence(s) remain" -f $unknownCount) }

# 3. Every referenced TASK-xxx / ADR-xxx has a file
$mdFiles = @($tracked | Where-Object { $_ -like '*.md' -and $_ -notlike 'orgins/*' })
$badRefs = @()
foreach ($f in $mdFiles) {
    $text = Get-Content -Raw -Path $f
    foreach ($m in [regex]::Matches($text, 'ADR-\d{3}')) {
        $id = $m.Value
        if (-not (Test-Path ("project/decisions/" + $id + "-*.md"))) { $badRefs += ($f + ' -> ' + $id) }
    }
    foreach ($m in [regex]::Matches($text, 'TASK-\d{3}')) {
        $id = $m.Value
        if (-not (Test-Path ("project/tasks/" + $id + ".md"))) { $badRefs += ($f + ' -> ' + $id) }
    }
}
if ($badRefs.Count -eq 0) { Add-Pass 'task/adr references' 'all referenced ids exist' }
else { Add-Warn 'task/adr references' ('unwritten or misspelled: ' + (($badRefs | Select-Object -Unique) -join '; ')) }

# 4. FEATURE_MATRIX status vocabulary and evidence rule
$allowed = @('NOT_STARTED','IN_PROGRESS','IMPLEMENTED_UNVERIFIED','VERIFIED','DEVICE_VERIFIED','BLOCKED','OUT_OF_SCOPE','-','—')
$matrixText = Get-Content -Raw -Path 'project/FEATURE_MATRIX.md'
$matrixLines = $matrixText -split "?
"
$badStates = @()
$verifiedRows = 0
foreach ($line in $matrixLines) {
    if ($line -notmatch '^\|\s*F-\d+') { continue }
    $cells = $line.Split('|') | ForEach-Object { $_.Trim() }
    if ($cells.Count -lt 7) {
        $badStates += ('malformed row: ' + $line.Substring(0, [Math]::Min(40, $line.Length)))
        continue
    }
    for ($i = 4; $i -le 6; $i++) {
        if ($cells.Count -gt $i) {
            $v = $cells[$i]
            if ($allowed -notcontains $v) { $badStates += ($cells[1] + ' = ' + $v) }
            if ($v -eq 'VERIFIED' -or $v -eq 'DEVICE_VERIFIED') { $verifiedRows++ }
        }
    }
}
if ($badStates.Count -eq 0) { Add-Pass 'feature matrix vocabulary' 'all statuses in the allowed set' }
else {
    $shown = ($badStates | Select-Object -First 5) -join '; '
    if ($badStates.Count -gt 5) { $shown = $shown + ' (+' + ($badStates.Count - 5) + ' more)' }
    Add-Fail 'feature matrix vocabulary' $shown
}

if ($verifiedRows -eq 0) { Add-Pass 'verified rows need evidence' 'no VERIFIED row yet' }
else {
    $testText = Get-Content -Raw -Path 'project/reports/TEST_REPORT.md'
    if ($testText -match 'PASS') { Add-Pass 'verified rows need evidence' ("{0} verified row(s), TEST_REPORT mentions PASS" -f $verifiedRows) }
    else { Add-Fail 'verified rows need evidence' ("{0} verified row(s) but TEST_REPORT has no PASS record" -f $verifiedRows) }
}

# 5. Handoff snapshot is filled in (no empty "Key:" placeholders left)
$handoff = Get-Content -Path 'project/handoffs/LAST_HANDOFF.md'
$inSnapshot = $false
$placeholders = @()
foreach ($line in $handoff) {
    if ($line -match '^##\s+Snapshot') { $inSnapshot = $true; continue }
    if ($inSnapshot -and $line -match '^##\s+') { break }
    if ($inSnapshot -and $line -match '^-\s*[^：:]+[：:]\s*$') { $placeholders += $line.Trim() }
}
if ($placeholders.Count -eq 0) { Add-Pass 'handoff snapshot filled' 'no empty field' }
else { Add-Fail 'handoff snapshot filled' (($placeholders | Select-Object -First 5) -join '; ') }

# 6. Relative markdown links resolve
$badLinks = @()
foreach ($f in $mdFiles) {
    $dir = Split-Path -Parent $f
    if (-not $dir) { $dir = '.' }
    $text = Get-Content -Raw -Path $f
    foreach ($m in [regex]::Matches($text, '\]\(([^)]+)\)')) {
        $target = $m.Groups[1].Value.Trim()
        if ($target -match '^(https?:|mailto:|#)') { continue }
        $target = ($target -split '#')[0]
        if (-not $target) { continue }
        $target = $target.Trim('<', '>')
        $resolved = Join-Path $dir $target
        if (-not (Test-Path $resolved)) { $badLinks += ($f + ' -> ' + $target) }
    }
}
if ($badLinks.Count -eq 0) { Add-Pass 'markdown links' 'all relative links resolve' }
else { Add-Fail 'markdown links' (($badLinks | Select-Object -Unique) -join '; ') }

# 7. No conflict markers / trailing whitespace in tracked text files
$textExt = @('.md','.ps1','.kts','.kt','.toml','.xml','.properties','.yml','.yaml')
$conflicts = @()
$trailing = @()
foreach ($f in $tracked) {
    if ($f -like 'orgins/*') { continue }
    $ext = [System.IO.Path]::GetExtension($f)
    if ($textExt -notcontains $ext -and $f -notlike '.gitignore') { continue }
    $i = 0
    foreach ($line in (Get-Content -Path $f)) {
        $i++
        if ($line -match '^(<{7}|>{7}|={7})') { $conflicts += ($f + ':' + $i) }
        if ($line -match '[ \t]+$') { $trailing += ($f + ':' + $i) }
    }
}
if ($conflicts.Count -eq 0) { Add-Pass 'no conflict markers' 'clean' }
else { Add-Fail 'no conflict markers' ($conflicts -join ', ') }
if ($trailing.Count -eq 0) { Add-Pass 'no trailing whitespace' 'clean' }
else { Add-Warn 'no trailing whitespace' (($trailing | Select-Object -First 5) -join ', ') }

# 8. Local input material must not be tracked
if ($tracked -contains 'temp.md') { Add-Fail 'local draft not tracked' 'temp.md is committed; it is input material, not a baseline' }
else { Add-Pass 'local draft not tracked' 'temp.md ignored' }

# 9. .gitignore covers build output and secrets
$ignore = Get-Content -Raw -Path '.gitignore'
$need = @('build/','.gradle/','local.properties','.idea/')
$miss = @($need | Where-Object { $ignore -notmatch [regex]::Escape($_) })
if ($miss.Count -eq 0) { Add-Pass 'gitignore covers build output' ($need -join ', ') }
else { Add-Fail 'gitignore covers build output' ("missing: " + ($miss -join ', ')) }

# 10. Release report must not claim PASS while no Gradle project exists
$hasGradle = (Test-Path 'gradlew.bat') -or (Test-Path 'gradlew')
$releaseText = Get-Content -Raw -Path 'project/reports/RELEASE_REPORT.md'
if (-not $hasGradle) {
    if ($releaseText -match 'PASS' -and $releaseText -notmatch 'NOT_RUN') {
        Add-Fail 'release report honest' 'release report claims PASS while no Gradle project exists'
    } else {
        Add-Pass 'release report honest' 'no Gradle project, no PASS claimed'
    }
} else {
    Add-Pass 'release report honest' 'Gradle project present; review release evidence manually'
}

# 11. Any commit that modifies a protected baseline file must also touch an ADR
$protected = @('AGENTS.md','ARCHITECTURE.md','DATABASE.md','DEVELOPMENT.md','project/REQUIREMENTS.md','README_GOVERNANCE.md')
$violations = @()
foreach ($c in @(git rev-list HEAD)) {
    $entries = @(git show --name-status --format= $c)
    $touched = @()
    $hasAdr = $false
    foreach ($e in $entries) {
        if ($e -match '^[AMD]\s+project/decisions/ADR-') { $hasAdr = $true }
        foreach ($p in $protected) {
            if ($e -match ('^[MD]\s+' + [regex]::Escape($p) + '$')) { $touched += $p }
        }
    }
    # A whitespace-only edit (e.g. trimming a trailing blank line) is not a baseline change.
    # Compare the two blobs with all whitespace removed instead of relying on diff flags.
    $realChanges = @()
    foreach ($p in $touched) {
        $parent = (git rev-list --parents -n 1 $c) -split ' '
        if ($parent.Count -lt 2) { $realChanges += $p; continue }
        $newText = ((git show ($c + ':' + $p)) -join '') -replace '\s', ''
        $oldText = ((git show ($parent[1] + ':' + $p)) -join '') -replace '\s', ''
        if ($newText -ne $oldText) { $realChanges += $p }
    }
    if ($realChanges.Count -gt 0 -and -not $hasAdr) {
        $violations += ($c.Substring(0, 7) + ': ' + ($realChanges -join ', '))
    }
}
if ($violations.Count -eq 0) { Add-Pass 'baseline changes carry an ADR' 'every commit touching a protected file also touches an ADR' }
else { Add-Fail 'baseline changes carry an ADR' ((($violations | Select-Object -First 5) -join '; ')) }

# Report
$fail = @($results | Where-Object { $_.Status -eq 'FAIL' })
$warn = @($results | Where-Object { $_.Status -eq 'WARN' })
$pass = @($results | Where-Object { $_.Status -eq 'PASS' })

Write-Host ''
Write-Host 'Governance consistency check' -ForegroundColor Cyan
Write-Host ("repo: " + $root)
Write-Host ("branch: " + (git branch --show-current) + " | commits: " + (git rev-list --count HEAD) + " | tracked: " + $tracked.Count)
Write-Host ''
foreach ($r in $results) {
    $color = 'Green'
    if ($r.Status -eq 'FAIL') { $color = 'Red' }
    if ($r.Status -eq 'WARN') { $color = 'Yellow' }
    Write-Host ("[{0}] {1,-42} {2}" -f $r.Status, $r.Name, $r.Detail) -ForegroundColor $color
}
Write-Host ''
Write-Host ("SUMMARY: {0} checks, {1} passed, {2} warnings, {3} failed" -f $results.Count, $pass.Count, $warn.Count, $fail.Count)
if ($fail.Count -gt 0) {
    Write-Host 'RESULT: FAILED' -ForegroundColor Red
    exit 1
}
Write-Host 'RESULT: OK' -ForegroundColor Green
exit 0
