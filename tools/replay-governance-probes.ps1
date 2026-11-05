# Replays the governance probes from REVIEW-TASK-015 and REVIEW-TASK-017, plus the ones added by
# TASK-020. Originals come from `git show HEAD:<path>` and restoration uses `git checkout --`, so a
# leftover dirty file can never be mistaken for the baseline (that exact mistake produced fake PASS
# lines while writing TASK-017).
#
# Usage: pwsh -NoProfile -File tools/replay-governance-probes.ps1
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
$bt = [char]96
$taskPath = 'project/tasks/TASK-019.md'
$reportPath = 'project/reports/TEST_REPORT.md'
$manifestPath = 'app/src/main/AndroidManifest.xml'
$catalogPath = 'gradle/libs.versions.toml'
$paths = @($taskPath, $reportPath, $manifestPath, $catalogPath)

function Get-Head([string]$p) { return ((git show ('HEAD:' + $p)) -join "`n") }
function Restore-All { foreach ($p in $paths) { git checkout -- $p } }
function Invoke-Check {
    $out = & pwsh -NoProfile -File tools/check-governance.ps1 2>&1
    return [pscustomobject]@{ Code = $LASTEXITCODE; Text = ($out -join "`n") }
}
$script:failures = 0
$script:count = 0
function Assert-Probe([string]$name, [string]$path, [string]$newText, [int]$expectCode, [string]$expectFragment) {
    $script:count++
    $head = Get-Head $path
    if ($newText -eq $head) {
        Write-Output ('[NOT APPLIED] ' + $name + ' - the mutation did not change ' + $path)
        $script:failures++
        return
    }
    try {
        Set-Content $path -Value $newText -NoNewline
        $r = Invoke-Check
    }
    finally { Restore-All }
    $codeOk = ($r.Code -eq $expectCode)
    $textOk = ($expectFragment -eq '' -or $r.Text.Contains($expectFragment))
    if ($codeOk -and $textOk) { Write-Output ('[OK] ' + $name + ' -> exit ' + $r.Code) }
    else {
        Write-Output ('[MISMATCH] ' + $name + ' -> exit ' + $r.Code + ' (expected ' + $expectCode + '), fragment ' + $textOk)
        ($r.Text -split "`n" | Select-String -Pattern '^\[FAIL\]' | ForEach-Object { Write-Output ('    ' + $_.Line) })
        $script:failures++
    }
}

Restore-All
$task = Get-Head $taskPath
$report = Get-Head $reportPath
$manifest = Get-Head $manifestPath
$catalog = Get-Head $catalogPath
$noteIdx = $task.IndexOf('## 11. Completion Review')

Assert-Probe 'P5 note row below the evidence row' $taskPath ($task.Insert($noteIdx, '| 备注 | 使用 verify-all.ps1 复跑 | — | — |' + "`n`n")) 0 ''
Assert-Probe 'P6 result column says 待补' $taskPath ($task.Replace('VERIFY ALL PASS', '待补')) 1 'result column states a verdict'
Assert-Probe 'P7 Latest Run drops the count' $reportPath ($report.Replace('单测 71 项 + DAO SQL 21 项', 'DAO SQL 21 项')) 1 'does not state 单测 N 项'
Assert-Probe 'P8 stray count quoted outside Runs' $reportPath ($report.Replace('## Known Test Gaps', "## Known Test Gaps`n`n- 历史记录：早期一次运行是 OK (59 tests)。")) 0 ''
Assert-Probe 'P10 result column states a failure' $taskPath ($task.Replace('VERIFY ALL PASS', 'VERIFY ALL FAILED')) 0 ''
Assert-Probe 'P11 Latest Run count disagrees with Runs' $reportPath ($report.Replace('单测 71 项 + DAO SQL 21 项', '单测 70 项 + DAO SQL 21 项')) 1 'but the last OK row is 71 tests'
Assert-Probe 'P12 note column repeats the placeholder text' $taskPath ($task.Insert($noteIdx, '| 备注 | 见上 | 未运行（任务未开始） | — |' + "`n`n")) 0 ''
Assert-Probe 'P13 result column is Chinese 通过' $taskPath ($task.Replace('VERIFY ALL PASS', '通过')) 1 'result column states a verdict'
Assert-Probe 'P14 Runs heading removed' $reportPath ($report.Replace('## Runs', '## Run log')) 1 'no ## Runs section'
$p15 = $report.Replace('单测 71 项 + DAO SQL 21 项', '单测 59 项 + DAO SQL 21 项').Replace('## Known Test Gaps', "## Known Test Gaps`n`n- 历史记录：早期一次运行是 OK (59 tests)。")
Assert-Probe 'P15 summary and a stray line both say 59' $reportPath $p15 1 'but the last OK row is 71 tests'
$notStarted = [regex]::Replace($task, '(?m)^- 状态：.*$', ('- 状态：' + $bt + 'NOT_STARTED' + $bt))
$p9 = $notStarted.Replace('VERIFY ALL PASS', '尚未开始').Replace('- [x] ', '- [ ] ')
$p9 = [regex]::Replace($p9, '(?m)^- \d{4}-.*$', '- 尚未开始')
Assert-Probe 'P9 NOT_STARTED with a placeholder row' $taskPath $p9 0 ''
Assert-Probe 'P16 NOT_STARTED but acceptance is ticked' $taskPath ($p9.Replace('- [ ] ', '- [x] ')) 1 'declares NOT_STARTED but shows work'
Assert-Probe 'P17 manifest declares INTERNET' $manifestPath ($manifest.Replace('android.permission.POST_NOTIFICATIONS', 'android.permission.INTERNET')) 1 'INTERNET permission'
Assert-Probe 'P18 catalog references a network library' $catalogPath ($catalog + "`nokhttp = '4.12.0'`n") 1 'network library'

Restore-All
$dirty = @(git status --porcelain -- $paths)
if ($dirty.Count -gt 0) { Write-Output ('[DIRTY] probe paths still modified: ' + ($dirty -join '; ')); $script:failures++ }
$baseline = Invoke-Check
$summary = ($baseline.Text -split "`n" | Select-String -Pattern 'SUMMARY:').Line
Write-Output ('baseline -> exit ' + $baseline.Code + ' (' + $summary + ')')
if ($baseline.Code -ne 0) { $script:failures++ }
Write-Output ('PROBES: ' + $script:count + ' run, ' + $script:failures + ' failed')
if ($script:failures -gt 0) { exit 1 }
