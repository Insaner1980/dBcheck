$ProjectCheckCommand = "codeql-check"
$SharedEntryPoint = "C:\Dev\Android-check\tools\InvokeProjectCheck.ps1"
if (-not (Test-Path -LiteralPath $SharedEntryPoint -PathType Leaf)) {
    Write-Error "PROJECT_CHECK_ENTRYPOINT_MISSING: $SharedEntryPoint" -ErrorAction Continue
    exit 2
}
& $SharedEntryPoint -ProjectCheckCommand $ProjectCheckCommand @args
exit $LASTEXITCODE
