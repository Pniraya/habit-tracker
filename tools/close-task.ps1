# Task close-out helper: generates a status summary block and (optionally) syncs README + status.
# Usage:  pwsh -NoProfile -File tools/close-task.ps1 -Task TASK-005 [-Apply]
param(
    [Parameter(Mandatory = $true)][string]$Task,
    [switch]$Apply,
    [string]$Repo
)
$ErrorActionPreference = "Stop"
if (-not $Repo) { $Repo = Split-Path -Parent $PSScriptRoot }
Set-Location $Repo
$tick = [char]96

$taskPath = if ($Task -match '\.md$') { $Task } else { "project/tasks/$Task.md" }
if (-not (Test-Path $taskPath)) { Write-Host "RESULT: task file not found: $taskPath" -ForegroundColor Red; exit 1 }
$taskId = [System.IO.Path]::GetFileNameWithoutExtension($taskPath)
$text = Get-Content -Raw -Path $taskPath

function Get-Field([string]$source, [string]$label, [string]$valuePattern) {
    $pattern = $label + '[^' + $tick + '\r\n]{0,12}' + [regex]::Escape($tick) + '(' + $valuePattern + ')' + [regex]::Escape($tick)
    $m = [regex]::Match($source, $pattern)
    if ($m.Success) { return $m.Groups[1].Value }
    return ""
}

$status = Get-Field $text '状态：' '[A-Z_]+'
$gate = Get-Field $text '阶段：' ('Gate [0-9][^' + $tick + '\r\n]*')
$gateToken = if ($gate -match 'Gate [0-9]') { $Matches[0] } else { "" }
# Only the Acceptance Criteria section is counted: other checkboxes (completion review,
# review template) are not acceptance items and used to inflate the numbers.
$acceptance = $text
$acceptanceMatch = [regex]::Match($text, '(?ms)^##+[^\r\n]*Acceptance Criteria[^\r\n]*\r?\n(.*?)(?=^##+ |\z)')
if ($acceptanceMatch.Success) { $acceptance = $acceptanceMatch.Groups[1].Value }
$done = ([regex]::Matches($acceptance, '(?m)^\s*- \[x\] ')).Count
$open = ([regex]::Matches($acceptance, '(?m)^\s*- \[ \] ')).Count

# Only the date and result of the newest run are embedded, so the summary cannot smuggle
# unrelated claims (for example an old ADR status) into this task file.
$lastRun = ""
# Governance-check rows are not test results, so they never become the "latest test record".
$runs = @(Get-Content -Path 'project/reports/TEST_REPORT.md' | Where-Object { $_ -match '^\|\s*20' } | Where-Object { $_ -notmatch 'check-governance' } | Where-Object { $_ -match 'run-unit-tests|verify-schema|verify-dao' })
if ($runs.Count -gt 0) {
    $cells = @($runs[-1].Split('|') | ForEach-Object { $_.Trim() })
    if ($cells.Count -ge 5) { $lastRun = $cells[1] + ' / ' + $cells[3] }
}

$block = @(
    '<!-- close-task:begin -->',
    '## 状态摘要（自动生成，勿手改）',
    '',
    ('- 任务：' + $taskId),
    ('- 状态：' + $tick + $status + $tick),
    ('- 阶段：' + $gate),
    ('- 验收：通过 ' + $done + ' 项 / 未通过 ' + $open + ' 项'),
    ('- 最近测试记录：' + $lastRun),
    '<!-- close-task:end -->'
) -join [Environment]::NewLine

if ($text -match '(?s)<!-- close-task:begin -->.*?<!-- close-task:end -->') {
    $text = [regex]::Replace($text, '(?s)<!-- close-task:begin -->.*?<!-- close-task:end -->', $block.Replace('$', '$$'))
} else {
    $text = $text.TrimEnd() + [Environment]::NewLine + [Environment]::NewLine + $block
}
$text = $text.TrimEnd() + [Environment]::NewLine
[System.IO.File]::WriteAllText((Resolve-Path $taskPath), $text)
Write-Host ("task summary written to " + $taskPath)

if ($Apply) {
    $readme = Get-Content -Raw -Path 'README.md'
    # 当前任务 is the task being worked on next, not the one just closed: rewriting only the id
    # here used to leave TASK-040's description under a TASK-030 label. Left to the maintainer.
    # $readme = [regex]::Replace($readme, '(\|\s*当前任务\s*\|\s*)' + [regex]::Escape($tick) + 'TASK-\d{3}' + [regex]::Escape($tick), ('$1' + $tick + $taskId + $tick))
    if ($gateToken) {
        $readme = [regex]::Replace($readme, '(\|\s*阶段\s*\|\s*)' + [regex]::Escape($tick) + 'Gate \d' + [regex]::Escape($tick), ('$1' + $tick + $gateToken + $tick))
    }
    [System.IO.File]::WriteAllText((Resolve-Path 'README.md'), $readme)

    $statusText = Get-Content -Raw -Path 'project/PROJECT_STATUS.md'
    # See the README note above: 当前任务 stays under manual control.
    # $statusText = [regex]::Replace($statusText, '(- 当前任务：)' + [regex]::Escape($tick) + 'TASK-\d{3}' + [regex]::Escape($tick), ('$1' + $tick + $taskId + $tick))
    if ($gateToken) {
        $statusText = [regex]::Replace($statusText, '(- 项目阶段：)' + [regex]::Escape($tick) + 'Gate \d' + [regex]::Escape($tick), ('$1' + $tick + $gateToken + $tick))
    }
    [System.IO.File]::WriteAllText((Resolve-Path 'project/PROJECT_STATUS.md'), $statusText)
    Write-Host ("synced README.md and project/PROJECT_STATUS.md to " + $taskId + " / " + $gateToken)
}

Write-Host ''
Write-Host 'Manual sync checklist (not automated on purpose):' -ForegroundColor Cyan
Write-Host '  [ ] project/reports/TEST_REPORT.md  - Latest Run 摘要与实际 Runs 最后一行一致'
Write-Host '  [ ] project/PROJECT_STATUS.md       - 最近验证结果、风险、在册例外'
Write-Host '  [ ] project/reports/RELEASE_REPORT.md - 构建证据不得与工程状态矛盾'
Write-Host '  [ ] project/handoffs/LAST_HANDOFF.md  - Snapshot 与下一安全操作'
Write-Host '  [ ] project/reviews/REVIEW-TASK-xxx.md - 独立 Reviewer 的 APPROVE / REJECT 结论'
Write-Host '  [ ] tools/check-governance.ps1      - 必须 RESULT: OK'
if ($status -eq 'PARTIAL') {
    Write-Host ('  [ ] PARTIAL 任务若留在 main，必须登记到 PROJECT_STATUS 的「在册例外」：' + $taskId) -ForegroundColor Yellow
}
Write-Host ''
Write-Host ("RESULT: SUMMARY WRITTEN (" + $taskId + ", " + $status + ")")
exit 0
