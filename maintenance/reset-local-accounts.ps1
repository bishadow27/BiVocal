param([switch]$Apply)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$backendRoot = Join-Path $projectRoot 'backend'
$maintenanceScript = Join-Path $PSScriptRoot 'reset-local-accounts.mjs'

Push-Location -LiteralPath $backendRoot
try {
    # Operator loads the existing DB configuration locally. It is never printed.
    & node --env-file=.env $maintenanceScript --preview
    if ($LASTEXITCODE -ne 0) { throw 'Preview failed. No reset was executed.' }
    if (-not $Apply) {
        Write-Host 'Preview only. Use -Apply to delete ALL accounts and create the new ADMIN.'
        return
    }

    Write-Host 'ALL existing accounts, progress, Quiz history, XP and streak will be removed.'
    Write-Host 'Topics and Flashcards will be preserved. New email: admin.new@bipassion.local'
    $securePassword = Read-Host 'New password (at least 8 characters)' -AsSecureString
    $secureConfirmation = Read-Host 'Repeat new password' -AsSecureString
    $plainPassword = [System.Net.NetworkCredential]::new('', $securePassword).Password
    $plainConfirmation = [System.Net.NetworkCredential]::new('', $secureConfirmation).Password
    if ($plainPassword -cne $plainConfirmation) { throw 'Passwords differ. No reset was executed.' }
    if ($plainPassword.Length -lt 8 -or [Text.Encoding]::UTF8.GetByteCount($plainPassword) -gt 72) {
        throw 'Password must have at least 8 characters and at most 72 UTF-8 bytes. No reset was executed.'
    }

    # ASCII transport preserves Unicode passwords on Windows PowerShell 5.1.
    # This is a secret stdin payload, not encryption; never write it to disk/logs.
    $passwordPayload = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($plainPassword))
    $passwordPayload | & node --env-file=.env $maintenanceScript --apply
    if ($LASTEXITCODE -ne 0) { throw 'Reset did not report success. Review the previous message before retrying.' }
    Write-Host 'Login email: admin.new@bipassion.local'
    Write-Host 'Password: the new password entered above.'
}
finally {
    Remove-Variable securePassword, secureConfirmation, plainPassword, plainConfirmation, passwordPayload -ErrorAction SilentlyContinue
    Pop-Location
}
