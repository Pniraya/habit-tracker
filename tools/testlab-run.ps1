<#
.SYNOPSIS
  Run the debug APK through Firebase Test Lab Robo tests on a Xiaomi and an OPPO
  device, then download the results.

.DESCRIPTION
  Needs the Google Cloud SDK (gcloud) on PATH, an authenticated account, and a
  Firebase project with billing enabled: Test Lab physical devices are only
  available on the Blaze plan. Nothing here runs offline, so every step checks
  its prerequisite and stops with a readable message instead of failing halfway.

.EXAMPLE
  pwsh -NoProfile -File tools/testlab-run.ps1 -Project my-firebase-project
#>
param(
  [Parameter(Mandatory = $true)][string]$Project,
  [string]$Apk = "app/build/outputs/apk/debug/app-debug.apk",
  [string]$OutDir = "project/reports/testlab",
  [string[]]$Models = @("Xiaomi", "OPPO"),
  [string]$Timeout = "5m"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
  Write-Host "gcloud not found. Install the Google Cloud SDK from https://cloud.google.com/sdk/docs/install and run gcloud auth login."
  exit 2
}

if (-not (Test-Path $Apk)) { Write-Host "APK missing: $Apk (run gradlew assembleDebug first)"; exit 2 }

$active = (gcloud config get-value account 2>$null) -join ""
if (-not $active -or $active -match "\(unset\)") { Write-Host "No authenticated account: run gcloud auth login first."; exit 2 }

Write-Host "Listing Test Lab devices..."
$listing = (gcloud firebase test android models list --project $Project --format="csv[no-heading](MODEL_ID,MAKE,NAME,OS_VERSION_IDS)" 2>&1) -join [Environment]::NewLine
if ($LASTEXITCODE -ne 0) { Write-Host $listing; exit $LASTEXITCODE }

$rows = $listing -split "\r?\n" | Where-Object { $_ -match "," } | ForEach-Object {
  $c = $_ -split ","
  [pscustomobject]@{ Model = $c[0]; Make = $c[1]; Name = $c[2]; Versions = $c[3] }
}

$picked = @()
foreach ($make in $Models) {
  $hit = $rows | Where-Object { $_.Make -match $make -or $_.Name -match $make } | Select-Object -First 1
  if ($hit) { $picked += $hit } else { Write-Host "No device found for $make" }
}

if ($picked.Count -eq 0) { Write-Host "No matching devices in this project; check gcloud firebase test android models list."; exit 3 }

$stamp = Get-Date -Format "yyyy-MM-dd-HHmm"
$results = Join-Path $OutDir $stamp
New-Item -ItemType Directory -Force $results | Out-Null

$failed = 0
foreach ($device in $picked) {
  $version = ($device.Versions -split ";")[-1]
  $safe = ($device.Name -replace "[^A-Za-z0-9]+", "-")
  $dir = Join-Path $results $safe
  Write-Host "=== $($device.Name) (model=$($device.Model), version=$version)"
  gcloud firebase test android run `
    --type robo `
    --app $Apk `
    --device "model=$($device.Model),version=$version,locale=zh_CN,orientation=portrait" `
    --timeout $Timeout `
    --project $Project 2>&1 | Tee-Object -Variable run
  if ($LASTEXITCODE -ne 0) { $failed++ }
  $run -join [Environment]::NewLine | Set-Content -Encoding utf8 (Join-Path $dir ".log") -ErrorAction SilentlyContinue
}

Write-Host "Results: $results"
if ($failed -gt 0) { Write-Host "RESULT: TESTLAB FAILED ($failed device run(s))"; exit 1 }
Write-Host "RESULT: TESTLAB PASS"