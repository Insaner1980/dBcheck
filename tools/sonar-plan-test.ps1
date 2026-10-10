#Requires -Version 5.1

$ErrorActionPreference = "Stop"
$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$fixtureRoot = Join-Path $tempBase ("sonar-plan-test-" + [Guid]::NewGuid().ToString("N"))

try {
    New-Item -ItemType Directory -Path (Join-Path $fixtureRoot ".git") -Force | Out-Null
    $source = Get-Content -LiteralPath (Join-Path $PSScriptRoot "sonar.ps1") -Raw
    $parseTokens = $null
    $parseErrors = $null
    $ast = [Management.Automation.Language.Parser]::ParseInput($source, [ref]$parseTokens, [ref]$parseErrors)
    if ($parseErrors.Count) { throw "Sonar script has parse errors." }
    $cliFunction = $ast.Find({ param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq "Invoke-SonarCli" }, $false)
    if ($null -eq $cliFunction) { throw "Sonar CLI function not found." }
    # Replace the external boundary; the fixture can never invoke the real CLI.
    $stub = 'function Invoke-SonarCli { param([string[]]$Arguments) Write-Output "FIXTURE_CLI_CALLED"; exit 77 }'
    $source = $source.Substring(0, $cliFunction.Extent.StartOffset) + $stub + $source.Substring($cliFunction.Extent.EndOffset)
    $scriptPath = Join-Path $fixtureRoot "sonar.ps1"
    Set-Content -LiteralPath $scriptPath -Value $source -Encoding utf8
    Set-Content -LiteralPath (Join-Path $fixtureRoot "sonar-project.properties") -Value "sonar.projectKey=fixture" -Encoding utf8
    $shellPath = (Get-Process -Id $PID).Path
    Push-Location -LiteralPath $fixtureRoot
    try {
        $cases = @(
            @{ Arguments = @("-PlanOnly"); ExitCode = 0 },
            @{ Arguments = @("-PlanOnly", "-AllowExternalUpload"); ExitCode = 0 },
            @{ Arguments = @("-PlanOnly", "-AllowExternalUpload", "list", "issues"); ExitCode = 0 },
            @{ Arguments = @("-PlanOnly", "list", "issues"); ExitCode = 0 },
            @{ Arguments = @("-AllowExternalUpload", "list", "issues"); ExitCode = 77 }
        )
        foreach ($case in $cases) {
            $output = & $shellPath -NoProfile -File $scriptPath @($case.Arguments) 2>&1
            $exitCode = $LASTEXITCODE
            if ($exitCode -ne $case.ExitCode) {
                throw "Sonar arguments '$($case.Arguments -join ' ')' returned $exitCode, expected $($case.ExitCode). $output"
            }
            if ($case.ExitCode -eq 0 -and ($output -join "`n").Contains("FIXTURE_CLI_CALLED")) {
                throw "Plan-only mode called the external boundary."
            }
            if (Test-Path -LiteralPath (Join-Path $fixtureRoot "reports")) {
                throw "Plan/CLI fixture unexpectedly wrote reports."
            }
        }
    }
    finally { Pop-Location }
    Write-Output "sonar-plan-test: PASS (4 plan cases; external stub control)"
}
finally {
    $resolvedRoot = [IO.Path]::GetFullPath($fixtureRoot)
    if (-not $resolvedRoot.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFileName($resolvedRoot) -notmatch '^sonar-plan-test-[0-9a-f]{32}$') {
        throw "Unexpected fixture cleanup path."
    }
    if (Test-Path -LiteralPath $resolvedRoot) { Remove-Item -LiteralPath $resolvedRoot -Recurse -Force }
}
