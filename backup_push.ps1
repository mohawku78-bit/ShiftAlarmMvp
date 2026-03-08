param(
    [string]$Message = ""
)

$ErrorActionPreference = "Stop"

$changes = git status --porcelain
if (-not $changes) {
    Write-Output "No changes to backup."
    exit 0
}

git add -A

if ([string]::IsNullOrWhiteSpace($Message)) {
    $Message = "backup: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')"
}

git commit -m $Message
git push

Write-Output "Backup complete."
