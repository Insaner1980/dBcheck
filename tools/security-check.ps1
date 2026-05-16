#Requires -Version 5.1
<#
    Repo-local security-check entrypoint.
    PowerShell-profiilin `sc` etsii ensisijaisesti tools/security-check.ps1-tiedostoa.
#>

[CmdletBinding()]
param(
    [string]$Root = (Get-Location).Path,
    [switch]$ResolveOnly,
    [switch]$WithoutDeps
)

$scriptPath = Join-Path (Split-Path -Parent $PSScriptRoot) 'scripts\security-check.ps1'

if ($ResolveOnly) {
    Write-Output (Resolve-Path -LiteralPath $Root).Path
    return
}

if ($WithoutDeps) {
    & $scriptPath -WithoutDeps
} else {
    & $scriptPath
}
