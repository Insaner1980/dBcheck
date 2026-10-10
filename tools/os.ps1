$ProjectCheckCommand = "osv-scan"
$ProjectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$SharedEntryPoint = "C:\Dev\Android-check\tools\InvokeProjectCheck.ps1"
if (-not (Test-Path -LiteralPath $SharedEntryPoint -PathType Leaf)) {
    Write-Error "PROJECT_CHECK_ENTRYPOINT_MISSING: $SharedEntryPoint" -ErrorAction Continue
    exit 2
}
& $SharedEntryPoint -ProjectCheckCommand $ProjectCheckCommand -Root $ProjectRoot @args
exit $LASTEXITCODE
