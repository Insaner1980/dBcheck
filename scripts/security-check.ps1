#Requires -Version 5.1

[CmdletBinding()]
param(
    [switch]$ResolveOnly,
    [switch]$WithoutDeps
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
$OutputEncoding = [Console]::OutputEncoding
$env:PYTHONUTF8 = "1"
$env:PYTHONIOENCODING = "utf-8"
if ([string]::IsNullOrWhiteSpace($env:NVD_API_KEY)) {
    $userNvdApiKey = [Environment]::GetEnvironmentVariable("NVD_API_KEY", "User")
    if (-not [string]::IsNullOrWhiteSpace($userNvdApiKey)) {
        $env:NVD_API_KEY = $userNvdApiKey
    }
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir
$ReportsDir = Join-Path $ProjectDir "reports"
$SemgrepTextReport = Join-Path $ReportsDir "security-code.txt"
$SemgrepJsonReport = Join-Path $ReportsDir "security-code.json"
$DependencyTextReport = Join-Path $ReportsDir "security-deps.txt"
$DependencyRawReport = Join-Path $ReportsDir "security-deps-raw.txt"
$DependencyJsonReport = Join-Path $ReportsDir "dependency-check-report.json"
$DependencyCheckTask = if ($env:DEPENDENCY_CHECK_TASK) { $env:DEPENDENCY_CHECK_TASK } else { ":app:dependencyCheckAnalyze" }
$SecurityCheckGradleHome = Join-Path $ProjectDir ".gradle\security-check-home"
$DependencyCheckDataDir = Join-Path $ProjectDir ".gradle\dependency-check-data"
$DependencyCheckDbFile = Join-Path $DependencyCheckDataDir "11.0\odc.mv.db"
$LegacyReportsGradleHome = Join-Path $ReportsDir ".gradle-home"

function Test-EnvFlag {
    param(
        [string]$Value,
        [bool]$Default = $false
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return $Default
    }

    return $Value -in @("1", "true", "yes", "on")
}

function Write-ReportHeader {
    param([string]$Path, [string]$Title, [string]$Command)

    Set-Content -LiteralPath $Path -Encoding utf8 -Value @(
        $Title
        "Root: $ProjectDir"
        "Command: $Command"
        "Started: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
        ""
    )
}

function Invoke-Semgrep {
    Write-ReportHeader -Path $SemgrepTextReport -Title "semgrep" -Command "reports/security-code.txt :: semgrep scan --config config/semgrep/dbcheck-security.yml --metrics=off --json"

    $semgrep = Get-Command semgrep -ErrorAction SilentlyContinue
    if ($null -eq $semgrep) {
        Add-Content -LiteralPath $SemgrepTextReport -Encoding utf8 -Value "SKIPPED: semgrep ei ole PATHissa."
        return 0
    }

    $configPath = Join-Path $ProjectDir "config\semgrep\dbcheck-security.yml"
    Push-Location -LiteralPath $ProjectDir
    try {
        & $semgrep.Source scan `
            --metrics=off `
            --quiet `
            --json `
            --config $configPath `
            --exclude app/build `
            --exclude build `
            --exclude reports `
            --disable-version-check `
            --output $SemgrepJsonReport `
            . 2>&1 | Tee-Object -FilePath $SemgrepTextReport -Append | Out-Host
        $code = if ($null -ne $global:LASTEXITCODE) { [int]$global:LASTEXITCODE } else { 0 }
    }
    catch {
        Add-Content -LiteralPath $SemgrepTextReport -Encoding utf8 -Value $_.Exception.Message
        $code = 1
    }
    finally {
        Pop-Location
    }

    if ($code -ne 0 -or -not (Test-Path -LiteralPath $SemgrepJsonReport)) {
        Add-Content -LiteralPath $SemgrepTextReport -Encoding utf8 -Value "Semgrep-skannaus epäonnistui."
        return $code
    }

    $data = Get-Content -Raw -Encoding utf8 -LiteralPath $SemgrepJsonReport | ConvertFrom-Json
    $results = @($data.results)
    $lines = @("== Semgrep security scan ==")
    if ($results.Count -eq 0) {
        $lines += "Ei löydöksiä."
    }
    else {
        $lines += "Löydöksiä: $($results.Count)"
        foreach ($item in $results) {
            $path = if ($item.path) { $item.path } else { "?" }
            $line = if ($item.start.line) { $item.start.line } else { "?" }
            $checkId = if ($item.check_id) { $item.check_id } else { "?" }
            $message = if ($item.extra.message) { $item.extra.message.Trim() } else { "" }
            $lines += "- ${path}:${line} [$checkId] $message"
        }
    }

    Set-Content -LiteralPath $SemgrepTextReport -Encoding utf8 -Value $lines
    $lines | ForEach-Object { Write-Host $_ }
    return 0
}

function Invoke-DependencyCheck {
    Write-ReportHeader -Path $DependencyTextReport -Title "dependency audit" -Command "reports/security-deps.txt :: OWASP dependency-check"
    Set-Content -LiteralPath $DependencyRawReport -Encoding utf8 -Value @()

    $enabled = -not $WithoutDeps -and (Test-EnvFlag -Value $env:DEPENDENCY_CHECK_ENABLED -Default $true)
    if (-not $enabled) {
        Add-Content -LiteralPath $DependencyTextReport -Encoding utf8 -Value "SKIPPED: dependency-check ohitettu."
        return 0
    }

    $autoUpdate = Test-EnvFlag -Value $env:DEPENDENCY_CHECK_AUTO_UPDATE -Default $true
    if (-not $autoUpdate -and -not (Test-Path -LiteralPath $DependencyCheckDbFile)) {
        Add-Content -LiteralPath $DependencyTextReport -Encoding utf8 -Value "Dependency-checkin paikallinen CVE-tietokanta puuttuu."
        Add-Content -LiteralPath $DependencyTextReport -Encoding utf8 -Value "Alusta se kerran: `$env:DEPENDENCY_CHECK_AUTO_UPDATE=`"true`"; sc"
        return 1
    }

    $gradle = Join-Path $ProjectDir "gradlew.bat"
    Push-Location -LiteralPath $ProjectDir
    try {
        Remove-LegacyReportsGradleHome
        $env:GRADLE_USER_HOME = $SecurityCheckGradleHome
        $env:DEPENDENCY_CHECK_DATA_DIRECTORY = $DependencyCheckDataDir
        $env:DEPENDENCY_CHECK_AUTO_UPDATE = if ($autoUpdate) { "true" } else { "false" }
        & $gradle "--no-daemon" $DependencyCheckTask "--no-configuration-cache" "--console=plain" *>&1 |
            Tee-Object -FilePath $DependencyRawReport |
            Out-Host
        $code = if ($null -ne $global:LASTEXITCODE) { [int]$global:LASTEXITCODE } else { 0 }
    }
    catch {
        Add-Content -LiteralPath $DependencyRawReport -Encoding utf8 -Value $_.Exception.Message
        $code = 1
    }
    finally {
        Pop-Location
    }

    if ($code -ne 0) {
        Add-Content -LiteralPath $DependencyTextReport -Encoding utf8 -Value "Dependency-check epäonnistui. Raakaloki: $DependencyRawReport"
        return $code
    }

    if (-not (Test-Path -LiteralPath $DependencyJsonReport)) {
        Add-Content -LiteralPath $DependencyTextReport -Encoding utf8 -Value "Yhteenvetoa ei voitu muodostaa: dependency-check-report.json puuttuu."
        Add-Content -LiteralPath $DependencyTextReport -Encoding utf8 -Value "Raakaloki: $DependencyRawReport"
        return 0
    }

    $data = Get-Content -Raw -Encoding utf8 -LiteralPath $DependencyJsonReport | ConvertFrom-Json
    $packageVulns = [ordered]@{}
    foreach ($dependency in @($data.dependencies)) {
        $vulns = @($dependency.vulnerabilities)
        if ($vulns.Count -eq 0) {
            continue
        }

        $vulnIds = @($vulns | ForEach-Object { if ($_.name) { $_.name } else { "?" } } | Sort-Object -Unique)
        $packages = @($dependency.packages | ForEach-Object { $_.id } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        if ($packages.Count -eq 0) {
            $packages = @($(if ($dependency.fileName) { $dependency.fileName } else { "?" }))
        }

        foreach ($packageId in $packages) {
            if (-not $packageVulns.Contains($packageId)) {
                $packageVulns[$packageId] = [System.Collections.Generic.HashSet[string]]::new()
            }
            foreach ($vulnId in $vulnIds) {
                [void]$packageVulns[$packageId].Add($vulnId)
            }
        }
    }

    $lines = @()
    if ($packageVulns.Count -eq 0) {
        $lines += "Ei löydöksiä."
    }
    else {
        $totalVulns = 0
        foreach ($set in $packageVulns.Values) {
            $totalVulns += $set.Count
        }
        $lines += "Löydöksiä: $($packageVulns.Count) pakettia, $totalVulns yksilöllistä CVE-viittausta"
        foreach ($packageId in ($packageVulns.Keys | Sort-Object)) {
            $cves = @($packageVulns[$packageId] | Sort-Object) -join ", "
            $lines += "- ${packageId}: $cves"
        }
    }
    $lines += "Raakaloki: $DependencyRawReport"

    Add-Content -LiteralPath $DependencyTextReport -Encoding utf8 -Value $lines
    $lines | ForEach-Object { Write-Host $_ }
    return 0
}

function Remove-LegacyReportsGradleHome {
    if (-not (Test-Path -LiteralPath $LegacyReportsGradleHome)) {
        return
    }

    $reportsPath = (Resolve-Path -LiteralPath $ReportsDir).Path
    $legacyPath = (Resolve-Path -LiteralPath $LegacyReportsGradleHome).Path
    if (-not $legacyPath.StartsWith($reportsPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove unexpected path: $legacyPath"
    }

    Remove-Item -LiteralPath $legacyPath -Recurse -Force
}

if ($ResolveOnly) {
    Write-Output $ProjectDir
    exit 0
}

New-Item -ItemType Directory -Force -Path $ReportsDir | Out-Null

$semgrepCode = Invoke-Semgrep
$dependencyCode = Invoke-DependencyCheck
$exitCode = if ($semgrepCode -ne 0) { $semgrepCode } elseif ($dependencyCode -ne 0) { $dependencyCode } else { 0 }

Write-Host ""
Write-Host "security-check summary"
Write-Host "Root: $ProjectDir"
Write-Host "semgrep: $(if ($semgrepCode -eq 0) { 'OK' } else { "FAILED ($semgrepCode)" }) -> $SemgrepTextReport"
Write-Host "dependency audit: $(if ($dependencyCode -eq 0) { 'OK' } else { "FAILED ($dependencyCode)" }) -> $DependencyTextReport"

exit $exitCode
