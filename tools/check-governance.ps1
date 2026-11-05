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
    'project/tasks/TASK_TEMPLATE.md','project/handoffs/LAST_HANDOFF.md','project/reviews/REVIEW_TEMPLATE.md',
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


# 12. README must agree with PROJECT_STATUS on stage and current task
$tick = [char]96
$readmeText = Get-Content -Raw -Path 'README.md'
$syncProblems = @()

$stTask = if ($statusText -match ('当前任务：\s*' + [regex]::Escape($tick) + '(TASK-\d{3})' + [regex]::Escape($tick))) { $Matches[1] } else { $null }
$rdTask = if ($readmeText -match ('\|\s*当前任务\s*\|\s*' + [regex]::Escape($tick) + '(TASK-\d{3})' + [regex]::Escape($tick))) { $Matches[1] } else { $null }
$stGate = if ($statusText -match ('项目阶段：\s*' + [regex]::Escape($tick) + '(Gate \d)' + [regex]::Escape($tick))) { $Matches[1] } else { $null }
$rdGate = if ($readmeText -match ('\|\s*阶段\s*\|\s*' + [regex]::Escape($tick) + '(Gate \d)' + [regex]::Escape($tick))) { $Matches[1] } else { $null }

if (-not $stTask) { $syncProblems += 'PROJECT_STATUS has no 当前任务 id' }
if (-not $rdTask) { $syncProblems += 'README has no 当前任务 id' }
if ($stTask -and $rdTask -and $stTask -ne $rdTask) { $syncProblems += ('README ' + $rdTask + ' vs status ' + $stTask) }
if (-not $stGate) { $syncProblems += 'PROJECT_STATUS has no 阶段 Gate token' }
if (-not $rdGate) { $syncProblems += 'README has no 阶段 Gate token' }
if ($stGate -and $rdGate -and $stGate -ne $rdGate) { $syncProblems += ('README ' + $rdGate + ' vs status ' + $stGate) }
if ($syncProblems.Count -eq 0) { Add-Pass 'readme matches status' ($stTask + ' / ' + $stGate) }
else { Add-Fail 'readme matches status' ($syncProblems -join '; ') }

# 13. ADR statuses must not contradict the documents that describe them
$adrStatus = @{}
foreach ($f in @(Get-ChildItem 'project/decisions' -Filter 'ADR-*.md' | Where-Object { $_.Name -ne 'ADR-TEMPLATE.md' })) {
    $adrText = Get-Content -Raw -Path $f.FullName
    $id = $f.BaseName.Substring(0, 7)
    if ($adrText -match ('- 状态：\s*' + [regex]::Escape($tick) + '([A-Z_]+)' + [regex]::Escape($tick))) { $adrStatus[$id] = $Matches[1] }
}
$coveredIds = @()
foreach ($m in [regex]::Matches($statusText, 'ADR-(\d{3})\D{1,6}ADR-(\d{3})')) {
    $from = [int]$m.Groups[1].Value
    $to = [int]$m.Groups[2].Value
    for ($i = $from; $i -le $to; $i++) { $coveredIds += ('ADR-' + $i.ToString('000')) }
}
foreach ($m in [regex]::Matches($statusText, 'ADR-\d{3}')) { $coveredIds += $m.Value }

$adrProblems = @()
foreach ($id in $adrStatus.Keys) {
    if ($coveredIds -notcontains $id) { $adrProblems += ($id + ' not covered by PROJECT_STATUS') }
}
$claimFiles = @('project/PROJECT_STATUS.md', 'README.md', 'project/handoffs/LAST_HANDOFF.md')
$claimFiles += @($tracked | Where-Object { $_ -like 'project/tasks/*.md' })
# Lines marked as historical (当时 / 历史记录) record what was true then; only present-tense claims must match.
foreach ($f in $claimFiles) {
    if (-not (Test-Path $f)) { continue }
    foreach ($line in (Get-Content -Path $f)) {
        if ($line -match '当时|历史记录') { continue }
        foreach ($m in [regex]::Matches($line, 'ADR-(\d{3})[^\r\n]{0,30}?(PROPOSED|ACCEPTED)')) {
            $id = 'ADR-' + $m.Groups[1].Value
            $claimed = $m.Groups[2].Value
            if ($adrStatus.ContainsKey($id) -and $adrStatus[$id] -ne $claimed) {
                $adrProblems += ($f + ': ' + $id + ' claims ' + $claimed + ', ADR file says ' + $adrStatus[$id])
            }
        }
    }
}
if ($adrProblems.Count -eq 0) { Add-Pass 'adr status consistent' ("{0} ADR(s) covered and uncontradicted" -f $adrStatus.Count) }
else { Add-Fail 'adr status consistent' (($adrProblems | Select-Object -Unique) -join '; ') }

# 14. Release report must not contradict an existing buildable project
$releaseReport = Get-Content -Raw -Path 'project/reports/RELEASE_REPORT.md'
$testReport = Get-Content -Raw -Path 'project/reports/TEST_REPORT.md'
$releaseProblems = @()
if ($hasGradle) {
    if ($releaseReport -match '尚无 Android 工程') { $releaseProblems += 'says no Android project while gradlew exists' }
    if ($testReport -match 'assembleDebug') {
        $hasSuccess = $false
        foreach ($line in ($testReport -split "\r?\n")) {
            if ($line -match 'assembleDebug' -and $line -match 'SUCCESSFUL') { $hasSuccess = $true }
        }
        if ($hasSuccess) {
            $debugRow = @($releaseReport -split "\r?\n" | Where-Object { $_ -match 'assembleDebug' }) | Select-Object -First 1
            if ($debugRow -and $debugRow -match 'NOT_RUN') { $releaseProblems += 'assembleDebug row still NOT_RUN' }
        }
    }
}
if ($releaseProblems.Count -eq 0) { Add-Pass 'release report not stale' 'build evidence does not contradict the project' }
else { Add-Fail 'release report not stale' ($releaseProblems -join '; ') }

# 15. TEST_REPORT Latest Run summary must match the last Runs row
$runRows = @($testReport -split "\r?\n" | Where-Object { $_ -match '^\|\s*20' })
$testProblems = @()
if ($runRows.Count -eq 0) { $testProblems += 'Runs table has no dated row' }
else {
    $lastRow = $runRows[-1]
    $cells = @($lastRow.Split('|') | ForEach-Object { $_.Trim() })
    if ($cells.Count -lt 5) { $testProblems += 'last Runs row is malformed' }
    else {
        $runDate = $cells[1]
        $runResult = $cells[3]
        $summaryBlock = (($testReport -split '## Runs')[0])
        if ($summaryBlock -notmatch [regex]::Escape($runDate)) { $testProblems += ('Latest Run does not mention ' + $runDate) }
        $expected = 'NOT_RUN'
        if ($runResult -match 'FAIL') { $expected = 'FAILED' }
        elseif ($runResult -match 'PASS|OK|SUCCESS') { $expected = 'PASS' }
        if ($summaryBlock -notmatch $expected) { $testProblems += ('Latest Run does not state ' + $expected + ' (last row: ' + $runResult + ')') }
        # The date and the PASS/FAILED word can agree while the count is stale (TASK-012 shipped a
        # summary saying 57 while the last run was 58). Compare the number too.
        # Two boundaries, both found by probes: only the ## Runs table is a run record, so an old
        # "OK (59 tests)" quoted in Known Gaps must not be compared (probe P8); and a summary that
        # omits the count entirely must fail instead of skipping the comparison (probe P7).
        $runsParts = @($testReport -split '## Runs')
        $runsBlock = if ($runsParts.Count -ge 2) { ($runsParts[1] -split "`n## ")[0] } else { '' }
        if ($summaryBlock -notmatch '单测\s*(\d+)\s*项') {
            $testProblems += 'Latest Run does not state 单测 N 项, so the count cannot be checked'
        }
        elseif (-not $runsBlock) {
            $testProblems += 'TEST_REPORT has no ## Runs section to compare the count against'
        }
        else {
            $claimedCount = [int]$Matches[1]
            $counts = @([regex]::Matches($runsBlock, 'OK \((\d+) tests\)') | ForEach-Object { [int]$_.Groups[1].Value })
            if ($counts.Count -eq 0) { $testProblems += 'Runs section has no OK (N tests) row' }
            elseif ($counts[-1] -ne $claimedCount) {
                $testProblems += ('Latest Run says 单测 ' + $claimedCount + ' 项 but the last OK row is ' + $counts[-1] + ' tests')
            }
        }
    }
}
if ($testProblems.Count -eq 0) { Add-Pass 'test report summary matches' 'Latest Run agrees with the last Runs row' }
else { Add-Fail 'test report summary matches' ($testProblems -join '; ') }

# 16. PARTIAL tasks on main must be registered as exceptions
$partialTasks = @()
foreach ($f in @(Get-ChildItem 'project/tasks' -Filter 'TASK-*.md')) {
    $t = Get-Content -Raw -Path $f.FullName
    if ($t -match ('- 状态：\s*' + [regex]::Escape($tick) + 'PARTIAL' + [regex]::Escape($tick))) { $partialTasks += $f.BaseName }
}
$currentBranch = (git branch --show-current).Trim()
if ($currentBranch -ne 'main') { Add-Pass 'partial tasks registered' ('not on main: ' + $currentBranch) }
elseif ($partialTasks.Count -eq 0) { Add-Pass 'partial tasks registered' 'no PARTIAL task in the tree' }
else {
    $unregistered = @($partialTasks | Where-Object { $statusText -notmatch [regex]::Escape($_) })
    if ($unregistered.Count -eq 0) { Add-Pass 'partial tasks registered' ('registered: ' + ($partialTasks -join ', ')) }
    else { Add-Fail 'partial tasks registered' ('missing from PROJECT_STATUS 在册例外: ' + ($unregistered -join ', ')) }
}

# 17. Reviewer checklist and record template cover the required dimensions
$checklistPath = 'project/REVIEW_CHECKLIST.md'
$reviewTemplatePath = 'project/reviews/REVIEW_TEMPLATE.md'
$requiredSections = @('Diff 核对', '架构一致性', '验证证据', '文档同步', '分支与提交', '结论')
if (-not (Test-Path $checklistPath) -or -not (Test-Path $reviewTemplatePath)) { Add-Fail 'reviewer checklist' 'checklist or review record template is missing' }
else {
    $checklistText = Get-Content -Raw -Path $checklistPath
    $missingSections = @($requiredSections | Where-Object { $checklistText -notmatch [regex]::Escape($_) })
    if ($missingSections.Count -eq 0) { Add-Pass 'reviewer checklist' 'all six review dimensions present' }
    else { Add-Fail 'reviewer checklist' ('missing sections: ' + ($missingSections -join ', ')) }
}

# 18. DONE tasks created after ADR-013 must retain an independent APPROVE record before main merge.
$reviewProblems = @()
foreach ($f in @(Get-ChildItem 'project/tasks' -Filter 'TASK-*.md')) {
    if ($f.BaseName -notmatch '^TASK-(\d{3})$') { continue }
    if ([int]$Matches[1] -lt 7) { continue }
    $taskText = Get-Content -Raw -Path $f.FullName
    if ($taskText -notmatch ('- 状态：\s*' + [regex]::Escape($tick) + 'DONE' + [regex]::Escape($tick))) { continue }
    $reviewPath = Join-Path 'project/reviews' ('REVIEW-' + $f.BaseName + '.md')
    if (-not (Test-Path $reviewPath)) { $reviewProblems += ($f.BaseName + ' has no review record'); continue }
    $reviewText = Get-Content -Raw -Path $reviewPath
    # Tolerate markdown emphasis around the value: a bolded APPROVE is still an APPROVE verdict.
    if ($reviewText -notmatch ('- 独立性：\s*\*{0,2}\s*' + [regex]::Escape($tick) + 'INDEPENDENT' + [regex]::Escape($tick))) { $reviewProblems += ($f.BaseName + ' review is not marked INDEPENDENT') }
    if ($reviewText -notmatch ('- 结论：\s*\*{0,2}\s*' + [regex]::Escape($tick) + 'APPROVE' + [regex]::Escape($tick))) { $reviewProblems += ($f.BaseName + ' review is not APPROVE') }
}
if ($reviewProblems.Count -eq 0) { Add-Pass 'done tasks have review evidence' 'all post-ADR-013 DONE tasks have independent APPROVE records' }
else { Add-Fail 'done tasks have review evidence' ($reviewProblems -join '; ') }

# 21. Tasks created after ADR-014 must record a verify-all run (TASK-009 onward).
$gateProblems = @()
foreach ($f in @(Get-ChildItem 'project/tasks' -Filter 'TASK-*.md')) {
    if ($f.BaseName -notmatch '^TASK-(\d{3})$') { continue }
    if ([int]$Matches[1] -lt 9) { continue }
    $taskText = Get-Content -Raw -Path $f.FullName
    # Anchor on the Verification Evidence table, not on any mention: a task that merely lists the
    # script in its Scope must not pass this check.
    $sections = $taskText -split '## 10\. Verification Evidence', 2
    if ($sections.Count -lt 2) { $gateProblems += ($f.BaseName + ' has no Verification Evidence section'); continue }
    $evidence = ($sections[1] -split "\n## ", 2)[0]
    # Evidence is a row whose RESULT column states a verdict. Naming the script is not evidence: a
    # row like `| script | 待补 |` used to pass (probe P6). Rows without a verdict, such as a planning
    # row or prose quoted into the table, are ignored wherever they sit (probe P5).
    $rows = @([regex]::Matches($evidence, '(?m)^\|[^\r\n]*verify-all\.ps1[^\r\n]*\|') | ForEach-Object { $_.Value })
    $verdicts = @()
    foreach ($r in $rows) {
        $rowCells = @($r.Split('|') | ForEach-Object { $_.Trim() })
        if ($rowCells.Count -lt 5) { continue }
        if ($rowCells[2] -match 'VERIFY ALL (PASS|FAILED)|^PASS$|^FAILED$|未运行（任务未开始）') {
            $verdicts += [pscustomobject]@{ Row = $r; Result = $rowCells[2] }
        }
    }
    # A task that has not started has no run to report; a placeholder row is tolerated there (probe P9).
    $notStarted = $taskText -match ('- 状态：\s*' + [regex]::Escape($tick) + 'NOT_STARTED' + [regex]::Escape($tick))
    if ($verdicts.Count -eq 0) {
        if (-not $notStarted) { $gateProblems += ($f.BaseName + ' has no verify-all row whose result column states a verdict') }
    }
    else {
        $lastVerdict = $verdicts[-1]
        if (-not $notStarted -and $lastVerdict.Result -match '未运行|未执行|尚未') {
            $gateProblems += ($f.BaseName + ' verify-all evidence row still says it never ran, but the task is past NOT_STARTED')
        }
    }
}
if ($gateProblems.Count -eq 0) { Add-Pass 'verify-all evidence' 'post-ADR-014 tasks record a verify-all run' }
else { Add-Fail 'verify-all evidence' ($gateProblems -join '; ') }

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
