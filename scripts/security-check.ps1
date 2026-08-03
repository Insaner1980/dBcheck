#Requires -Version 5.1

[CmdletBinding()]
param(
    [switch]$ResolveOnly,
    [switch]$PlanOnly,
    [switch]$Full,
    [switch]$WithoutDeps,
    [switch]$InitVerification,
    [switch]$History
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
$canonicalEntrypoint = Join-Path $projectRoot "tools\sc.ps1"

if (-not (Test-Path -LiteralPath $canonicalEntrypoint -PathType Leaf)) {
    Write-Error "SECURITY_CHECK_ENTRYPOINT_MISSING: $canonicalEntrypoint"
    exit 2
}

$arguments = @()
foreach ($entry in $PSBoundParameters.GetEnumerator()) {
    if ([bool]$entry.Value) {
        $arguments += "-$($entry.Key)"
    }
}

& $canonicalEntrypoint @arguments
exit $LASTEXITCODE
