#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$fixtureRoot = Join-Path $tempBase ("sonar-scm-test-" + [Guid]::NewGuid().ToString("N"))
$originalToken = [Environment]::GetEnvironmentVariable("SONAR_TOKEN", "Process")
$originalPath = [Environment]::GetEnvironmentVariable("PATH", "Process")

try {
    New-Item -ItemType Directory -Path (Join-Path $fixtureRoot ".git") -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot "sonar.ps1") -Destination (Join-Path $fixtureRoot "sonar.ps1")
    Set-Content -LiteralPath (Join-Path $fixtureRoot "sonar-project.properties") -Encoding utf8 -Value "sonar.projectKey=fixture"
    # Model the failing scanner boundary without uploading source or running Gradle.
    Set-Content -LiteralPath (Join-Path $fixtureRoot "gradlew.bat") -Encoding ascii -Value @'
@echo off
echo %* | findstr /L /C:"-Dsonar.scm.disabled=true" >nul
if not errorlevel 1 exit /b 92
echo %* | findstr /L /C:"-Dsonar.scm.use.blame.algorithm=GIT_NATIVE_BLAME" >nul
if errorlevel 1 (
    echo JGit fixture: Missing blob 162fb4a897d178c2279e11f243526f374f086d44 1>&2
    exit /b 91
)
echo Native Git blame fixture: PASS
exit /b 0
'@

    $shellPath = (Get-Process -Id $PID).Path
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", "fixture-token", "Process")
    [Environment]::SetEnvironmentVariable("PATH", (Join-Path $env:SystemRoot "System32"), "Process")
    Push-Location -LiteralPath $fixtureRoot
    try {
        $output = & $shellPath -NoProfile -File (Join-Path $fixtureRoot "sonar.ps1") -AllowExternalUpload *>&1
        $exitCode = $LASTEXITCODE
    }
    finally { Pop-Location }

    if ($exitCode -ne 0) {
        throw "Native Git blame scan returned $exitCode, expected 0. $($output -join [Environment]::NewLine)"
    }
    $report = Get-Content -LiteralPath (Join-Path $fixtureRoot "reports\sonar.txt") -Raw
    if (-not $report.Contains("Native Git blame fixture: PASS") -or $report.Contains("SONAR_ANALYSIS_FAILED")) {
        throw "Sonar report did not confirm the successful native Git blame scan."
    }
    Write-Output "sonar-scm-test: PASS (native blame selected; SCM remains enabled)"
}
finally {
    [Environment]::SetEnvironmentVariable("SONAR_TOKEN", $originalToken, "Process")
    [Environment]::SetEnvironmentVariable("PATH", $originalPath, "Process")
    $resolvedRoot = [IO.Path]::GetFullPath($fixtureRoot)
    if (-not $resolvedRoot.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFileName($resolvedRoot) -notmatch '^sonar-scm-test-[0-9a-f]{32}$') {
        throw "Unexpected fixture cleanup path."
    }
    if (Test-Path -LiteralPath $resolvedRoot) { Remove-Item -LiteralPath $resolvedRoot -Recurse -Force }
}
