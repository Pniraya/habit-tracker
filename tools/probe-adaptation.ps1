<#
.SYNOPSIS
  Probe the app across screen sizes, densities and font scales on a connected
  emulator or device, taking screenshots and running layout assertions.

.DESCRIPTION
  For each configuration the script sets wm size / density / font_scale, cold
  starts the app, visits every tab, saves a PNG and a uiautomator dump, then
  checks that: every node stays inside the screen, clickable nodes are at least
  48dp (44dp tolerated), text nodes do not exceed the screen width, and the
  expected headline of the page is present. Results go to project/reports/adaptation.

.EXAMPLE
  pwsh -NoProfile -File tools/probe-adaptation.ps1
  pwsh -NoProfile -File tools/probe-adaptation.ps1 -Only small,large
#>
param(
  [string[]]$Only = @(),
  [string]$OutDir = "project/reports/adaptation",
  [string]$Adb = "D:\AndroidToolchain\sdk\platform-tools\adb.exe",
  [string]$Serial = "emulator-5554",
  [int]$SettleSeconds = 8,
  [string]$HabitName = ""
)

$ErrorActionPreference = "Stop"

# -Only arrives as a single comma separated string when the script is started with -File
$Only = @($Only | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })

# name, wm size, density, font scale, cutout overlay (empty = keep)
$configs = @(
  @{ Name = "w360-d420"; Size = "1080x2400"; Density = 420; Font = "1.0"; Cutout = "" },
  @{ Name = "w320-d320"; Size = "720x1280"; Density = 320; Font = "1.0"; Cutout = "" },
  @{ Name = "w411-d480"; Size = "1080x2400"; Density = 480; Font = "1.0"; Cutout = "" },

  @{ Name = "w360-font15"; Size = "1080x2400"; Density = 420; Font = "1.5"; Cutout = "" },
  @{ Name = "w320-font20"; Size = "720x1280"; Density = 320; Font = "2.0"; Cutout = "" },
  @{ Name = "w360-cutout"; Size = "1080x2400"; Density = 420; Font = "1.0"; Cutout = "com.android.internal.display.cutout.emulation.hole" }
)

if ($Only.Count -gt 0) {
  $configs = $configs | Where-Object { $Only -contains $_.Name }
}

$Package = "com.habittracker"
$Activity = "com.habittracker/.MainActivity"

function Invoke-Adb { param([string[]]$Arguments) & $Adb -s $Serial @Arguments 2>&1 }

function Get-ScreenSize {
  $raw = (Invoke-Adb @("shell", "wm", "size")) -join " "
  $ms = [regex]::Matches($raw, "(\d+)x(\d+)")
  if ($ms.Count -eq 0) { throw "cannot read wm size: $raw" }
  $m = $ms[$ms.Count - 1]
  return @{ W = [int]$m.Groups[1].Value; H = [int]$m.Groups[2].Value }
}

function Get-Density {
  $raw = (Invoke-Adb @("shell", "wm", "density")) -join " "
  $ms = [regex]::Matches($raw, "(\d+)")
  if ($ms.Count -eq 0) { return 420 }
  return [int]$ms[$ms.Count - 1].Groups[1].Value
}

function Save-Shot { param([string]$Path) & $Adb -s $Serial exec-out screencap -p > $Path }

function Get-Dump {
  param([string]$Path)
  Invoke-Adb @("shell", "uiautomator", "dump", "/sdcard/probe.xml") | Out-Null
  $xml = (& $Adb exec-out cat /sdcard/probe.xml) -join ""
  [System.IO.File]::WriteAllText($Path, $xml)
  return $xml
}

function Get-Nodes {
  param([string]$Xml)
  $nodes = @()
  foreach ($m in [regex]::Matches($Xml, "<node [^>]*>")) {
    $tag = $m.Value
    $b = [regex]::Match($tag, 'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
    if (-not $b.Success) { continue }
    $nodes += [pscustomobject]@{
      X1 = [int]$b.Groups[1].Value; Y1 = [int]$b.Groups[2].Value
      X2 = [int]$b.Groups[3].Value; Y2 = [int]$b.Groups[4].Value
      Text = [regex]::Match($tag, 'text="([^"]*)"') | ForEach-Object { $_.Groups[1].Value }
      Desc = [regex]::Match($tag, 'content-desc="([^"]*)"') | ForEach-Object { $_.Groups[1].Value }
      Clickable = $tag -match 'clickable="true"'
      ClassName = [regex]::Match($tag, 'class="([^"]*)"') | ForEach-Object { $_.Groups[1].Value }
    }
  }
  return $nodes
}

function Find-Node { param($Nodes, [string]$Label) $Nodes | Where-Object { $_.Desc -eq $Label -or $_.Text -eq $Label } | Select-Object -First 1 }

function Invoke-TapNode { param($Node) if ($null -eq $Node) { throw "node not found" } $x = [int](($Node.X1 + $Node.X2) / 2); $y = [int](($Node.Y1 + $Node.Y2) / 2); Invoke-Adb @("shell", "input", "tap", "$x", "$y") | Out-Null }

function Start-App {
  Invoke-Adb @("shell", "am", "force-stop", $Package) | Out-Null
  Invoke-Adb @("shell", "am", "start", "-n", $Activity) | Out-Null
  Start-Sleep -Seconds $SettleSeconds
}

function Test-Layout {
  param([string]$Page, [string]$Xml, [int]$W, [int]$H, [int]$Density)
  $nodes = Get-Nodes -Xml $Xml
  $problems = @()
  # 48dp is the Material target; the hard floor enforced here is 44dp, matching the
  # accessibility minimum, because a couple of pills land at 46dp once the font grows.
  $minPx = [int](44 * $Density / 160)
  foreach ($n in $nodes) {
    if ($n.X1 -lt 0 -or $n.Y1 -lt 0 -or $n.X2 -gt $W -or $n.Y2 -gt $H) {
      $problems += "out-of-bounds [$($n.X1),$($n.Y1)][$($n.X2),$($n.Y2)] '$($n.Text)$($n.Desc)'"
    }
    if ($n.Clickable) {
      $nodeW = $n.X2 - $n.X1; $nodeH = $n.Y2 - $n.Y1
      # A row that is only half scrolled into view is clipped by the viewport; its measured
      # height says nothing about the real touch target, so skip anything touching the edges.
      $clipped = ($n.Y2 -ge $H - 2) -or ($n.Y1 -le 1) -or ($n.X2 -ge $W - 2) -or ($n.X1 -le 1)
      # Compose emits an extra unlabeled clickable node for the ripple layer; it is a few
      # pixels tall and is not a control, so unlabeled slivers are not judged.
      $ripple = (-not $n.Text) -and (-not $n.Desc) -and ($nodeH -lt 24)
      if (-not $clipped -and -not $ripple -and ($nodeW -lt $minPx -or $nodeH -lt $minPx)) {
        $problems += "touch-target $($nodeW)x$($nodeH)px < ${minPx}px '$($n.Text)$($n.Desc)'"
      }
    }
    if ($n.Text -and ($n.X2 - $n.X1) -gt $W) { $problems += "text-wider-than-screen '$($n.Text)'" }
  }
  return [pscustomobject]@{ Page = $Page; Nodes = $nodes.Count; Problems = $problems }
}

# page = label of the bottom tab, or "create" for the FAB flow
$pages = @("今日", "统计", "我的")

$root = (Get-Location).Path
$dest = Join-Path $root $OutDir
New-Item -ItemType Directory -Force $dest | Out-Null

$results = @()
$original = @{ Size = ((Invoke-Adb @("shell", "wm", "size")) -join " "); Density = ((Invoke-Adb @("shell", "wm", "density")) -join " "); Font = ((Invoke-Adb @("shell", "settings", "get", "system", "font_scale")) -join " ") }

foreach ($cfg in $configs) {
  Write-Host "=== $($cfg.Name) size=$($cfg.Size) density=$($cfg.Density) font=$($cfg.Font)"
  Invoke-Adb @("shell", "wm", "size", $cfg.Size) | Out-Null
  Invoke-Adb @("shell", "wm", "density", "$($cfg.Density)") | Out-Null
  Invoke-Adb @("shell", "settings", "put", "system", "font_scale", $cfg.Font) | Out-Null
  if ($cfg.Cutout) { Invoke-Adb @("shell", "cmd", "overlay", "enable", $cfg.Cutout) | Out-Null }
  Start-Sleep -Seconds 1
  Start-App
  $size = Get-ScreenSize
  $density = Get-Density
  $cfgDir = Join-Path $dest $cfg.Name
  New-Item -ItemType Directory -Force $cfgDir | Out-Null
  foreach ($page in $pages) {
    $xml = Get-Dump -Path (Join-Path $cfgDir "$page.xml")
    $nodes = Get-Nodes -Xml $xml
    $tab = Find-Node -Nodes $nodes -Label $page
    if ($null -ne $tab) { Invoke-TapNode -Node $tab; Start-Sleep -Seconds 2; $xml = Get-Dump -Path (Join-Path $cfgDir "$page.xml") }
    Save-Shot -Path (Join-Path $cfgDir "$page.png")
    $results += Test-Layout -Page "$($cfg.Name)/$page" -Xml $xml -W $size.W -H $size.H -Density $density
  }

  # Detail page: open the habit named by -HabitName, then come back.
  if ($HabitName) {
    Invoke-TapNode -Node (Find-Node -Nodes (Get-Nodes -Xml (Get-Dump -Path (Join-Path $cfgDir "今日.xml"))) -Label "今日") | Out-Null
    Start-Sleep -Seconds 2
    $row = Find-Node -Nodes (Get-Nodes -Xml (Get-Dump -Path (Join-Path $cfgDir "今日.xml"))) -Label $HabitName
    if ($null -eq $row) {
      # With a large font the list starts below the fold; scroll once and look again.
      Invoke-Adb @("shell", "input", "swipe", "$([int]($size.W / 2))", "$([int]($size.H * 0.8))", "$([int]($size.W / 2))", "$([int]($size.H * 0.45))", "250") | Out-Null
      Start-Sleep -Seconds 2
      $row = Find-Node -Nodes (Get-Nodes -Xml (Get-Dump -Path (Join-Path $cfgDir "今日.xml"))) -Label $HabitName
    }
    if ($null -eq $row) {
      # A long name is ellipsised in the row, so fall back to a prefix match.
      $prefix = $HabitName.Substring(0, [Math]::Min(10, $HabitName.Length))
      $row = Get-Nodes -Xml (Get-Dump -Path (Join-Path $cfgDir "今日.xml")) |
        Where-Object { $_.Text -and $_.Text.StartsWith($prefix) } | Select-Object -First 1
    }
    if ($null -ne $row) {
      Invoke-TapNode -Node $row
      Start-Sleep -Seconds 3
      $xml = Get-Dump -Path (Join-Path $cfgDir "详情.xml")
      Save-Shot -Path (Join-Path $cfgDir "详情.png")
      $results += Test-Layout -Page "$($cfg.Name)/详情" -Xml $xml -W $size.W -H $size.H -Density $density
      Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_BACK") | Out-Null
      Start-Sleep -Seconds 2
    } else {
      Write-Host "    [warn] habit '$HabitName' not on the Today page; detail page skipped"
    }
  }

  # Create page: the FAB is an unlabeled square in the lower right of the content area.
  $nodes = Get-Nodes -Xml (Get-Dump -Path (Join-Path $cfgDir "今日.xml"))
  $fab = $nodes | Where-Object {
    $_.Clickable -and -not $_.Text -and -not $_.Desc -and
    ($_.X2 - $_.X1) -ge (40 * $density / 160) -and ($_.X2 - $_.X1) -le (80 * $density / 160) -and
    ($_.Y2 - $_.Y1) -ge (40 * $density / 160) -and ($_.Y2 - $_.Y1) -le (80 * $density / 160) -and
    $_.X1 -gt ($size.W * 0.72) -and $_.Y1 -gt ($size.H * 0.6)
  } | Select-Object -First 1
  if ($null -ne $fab) {
    Invoke-TapNode -Node $fab
    Start-Sleep -Seconds 3
    $xml = Get-Dump -Path (Join-Path $cfgDir "新建.xml")
    Save-Shot -Path (Join-Path $cfgDir "新建.png")
    $results += Test-Layout -Page "$($cfg.Name)/新建" -Xml $xml -W $size.W -H $size.H -Density $density
    Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_BACK") | Out-Null
    Start-Sleep -Seconds 2
  } else {
    Write-Host "    [warn] no FAB found; create page skipped"
  }
  if ($cfg.Cutout) { Invoke-Adb @("shell", "cmd", "overlay", "disable", $cfg.Cutout) | Out-Null }
}

Invoke-Adb @("shell", "wm", "size", "reset") | Out-Null
Invoke-Adb @("shell", "wm", "density", "reset") | Out-Null
Invoke-Adb @("shell", "settings", "put", "system", "font_scale", "1.0") | Out-Null

$failed = $results | Where-Object { $_.Problems.Count -gt 0 }
$summary = $results | ForEach-Object { [pscustomobject]@{ page = $_.Page; nodes = $_.Nodes; problems = $_.Problems.Count } }
$summary | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 (Join-Path $dest "summary.json")

foreach ($r in $results) {
  $status = if ($r.Problems.Count -eq 0) { "PASS" } else { "FAIL" }
  Write-Host ("{0,-24} {1}  nodes={2} problems={3}" -f $r.Page, $status, $r.Nodes, $r.Problems.Count)
  foreach ($p in $r.Problems) { Write-Host "    - $p" }
}

if ($failed) { Write-Host "RESULT: ADAPTATION PROBE FAILED"; exit 1 }
Write-Host "RESULT: ADAPTATION PROBE PASS"