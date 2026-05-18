$scriptPath = Join-Path $PSScriptRoot "sc.ps1"
& $scriptPath @args
exit $LASTEXITCODE
