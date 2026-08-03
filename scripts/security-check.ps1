#Requires -Version 5.1

$ProjectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$CanonicalEntryPoint = Join-Path $ProjectRoot "tools\sc.ps1"
if (-not (Test-Path -LiteralPath $CanonicalEntryPoint -PathType Leaf)) {
    Write-Error "SECURITY_CHECK_ENTRYPOINT_MISSING: $CanonicalEntryPoint"
    exit 2
}

& $CanonicalEntryPoint @args
exit $LASTEXITCODE
