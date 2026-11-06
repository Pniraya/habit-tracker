# Runs the JVM unit tests without Gradle test workers (named pipes are blocked in this sandbox).
# Usage:  pwsh -NoProfile -File tools/run-unit-tests.ps1
param(
    [string]$Repo
)
$ErrorActionPreference = "Stop"
if (-not $Repo) { $Repo = Split-Path -Parent $PSScriptRoot }
# Toolchain is environment-specific; prefer the caller's environment, then an explicit script.
if (-not $env:ANDROID_HOME -or -not $env:JAVA_HOME) {
    $candidates = @($env:HABIT_ENV_SCRIPT, (Join-Path (Split-Path -Parent $Repo) '..\AndroidToolchain\env.ps1'))
    $loaded = $false
    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) { . $candidate | Out-Null; $loaded = $true; break }
    }
    if (-not $loaded) {
        Write-Output "RESULT: TOOLCHAIN NOT CONFIGURED (set ANDROID_HOME/JAVA_HOME or HABIT_ENV_SCRIPT)"
        exit 1
    }
}

function Find-Jar([string]$Pattern) {
    $hit = Get-ChildItem (Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1") -Recurse -Filter $Pattern -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $hit) { throw ("jar not found in gradle cache: " + $Pattern) }
    return $hit.FullName
}

Push-Location $Repo
try {
    # Keep the transient compiler log under Gradle's ignored project directory.
    # The system TEMP directory can be unavailable in constrained environments.
    $log = Join-Path $Repo ".gradle\unit-test-compile.log"
    & ".\gradlew.bat" compileDebugUnitTestKotlin --console=plain --no-daemon *> $log
    if ($LASTEXITCODE -ne 0) {
        Get-Content $log | Select-String "^e: |error:" | Select-Object -First 15 | ForEach-Object { $_.Line }
        Write-Output ("RESULT: COMPILE FAILED (exit " + $LASTEXITCODE + ")")
        exit 1
    }

    $junit = Find-Jar "junit-4.13.2.jar"
    $hamcrest = Find-Jar "hamcrest-core-1.3.jar"
    $kotlinStdlib = Find-Jar "kotlin-stdlib-2.0.21.jar"
    $annotations = Find-Jar "annotations-23.0.0.jar"
    # Use cases are suspend functions, so the runner needs coroutines on the classpath.
    $coroutines = Find-Jar "kotlinx-coroutines-core-jvm-*.jar"

    $mainClasses = Join-Path $Repo "app\build\tmp\kotlin-classes\debug"
    $testClasses = Join-Path $Repo "app\build\tmp\kotlin-classes\debugUnitTest"

    $names = @()
    Get-ChildItem $testClasses -Recurse -Filter "*Test.class" | ForEach-Object {
        $relative = $_.FullName.Substring($testClasses.Length + 1)
        $relative = $relative -replace "\\", "." -replace "\.class$", ""
        if ($relative -notmatch "\$") { $names += $relative }
    }
    if ($names.Count -eq 0) { Write-Output "RESULT: NO TEST CLASSES"; exit 1 }

    $fullClasspath = @($mainClasses, $testClasses, $junit, $hamcrest, $kotlinStdlib, $annotations, $coroutines) -join ";"
    Write-Output ("tests: " + ($names -join ", "))
    Write-Output ""
    & "java" -cp $fullClasspath "org.junit.runner.JUnitCore" @names
    $code = $LASTEXITCODE
    Write-Output ""
    if ($code -eq 0) { Write-Output "RESULT: UNIT TESTS PASS" } else { Write-Output ("RESULT: UNIT TESTS FAILED (exit " + $code + ")") }
    exit $code
} finally {
    Pop-Location
}
