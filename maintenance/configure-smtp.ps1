param(
    [string]$EnvPath = (Join-Path $PSScriptRoot '..\backend\.env')
)

$ErrorActionPreference = 'Stop'
$resolvedEnvPath = [System.IO.Path]::GetFullPath($EnvPath)
$expectedBackend = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\backend'))
$envDirectory = [System.IO.Path]::GetDirectoryName($resolvedEnvPath)

if ($envDirectory -ne $expectedBackend) {
    throw 'EnvPath phai tro truc tiep toi file trong thu muc backend.'
}
if (-not (Test-Path -LiteralPath $resolvedEnvPath)) {
    throw "Khong tim thay file cau hinh: $resolvedEnvPath"
}

$smtpUser = (Read-Host 'Nhap dia chi Gmail dung de gui email').Trim()
if ($smtpUser -notmatch '^[^\s@]+@[^\s@]+\.[^\s@]+$') {
    throw 'Dia chi email khong hop le.'
}
$securePassword = Read-Host 'Nhap Google App Password (khong dung mat khau Gmail thuong)' -AsSecureString
$plainPassword = [System.Net.NetworkCredential]::new('', $securePassword).Password.Replace(' ', '')
if ($plainPassword.Length -lt 8) {
    throw 'App Password khong hop le.'
}

function Set-EnvValue {
    param([string[]]$Lines, [string]$Name, [string]$Value)
    $replacement = "$Name=$Value"
    $found = $false
    $updated = foreach ($line in $Lines) {
        if ($line -match ('^' + [regex]::Escape($Name) + '=')) {
            $found = $true
            $replacement
        } else {
            $line
        }
    }
    if (-not $found) { $updated += $replacement }
    return [string[]]$updated
}

try {
    [string[]]$lines = Get-Content -LiteralPath $resolvedEnvPath
    $settings = [ordered]@{
        SMTP_HOST       = 'smtp.gmail.com'
        SMTP_PORT       = '587'
        SMTP_SECURE     = 'false'
        SMTP_USER       = $smtpUser
        SMTP_PASSWORD   = $plainPassword
        MAIL_FROM       = "BiVoca <$smtpUser>"
        PUBLIC_RESET_URL = 'http://127.0.0.1:3000/reset-password'
    }
    foreach ($entry in $settings.GetEnumerator()) {
        $lines = Set-EnvValue -Lines $lines -Name $entry.Key -Value $entry.Value
    }
    [System.IO.File]::WriteAllLines($resolvedEnvPath, $lines, [System.Text.UTF8Encoding]::new($false))
    Write-Host 'Da luu cau hinh SMTP vao backend\.env. App Password khong duoc hien thi.'
    Write-Host 'PUBLIC_RESET_URL hien chi dung duoc tren chinh may tinh nay.'
} finally {
    $plainPassword = $null
    Remove-Variable securePassword -ErrorAction SilentlyContinue
}
