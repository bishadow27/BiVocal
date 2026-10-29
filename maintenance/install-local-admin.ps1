param([switch]$Uninstall)
$ErrorActionPreference = 'Stop'

$launcherFolder = $PSScriptRoot
$startupFolder = [Environment]::GetFolderPath('Startup')
$startupLink = Join-Path $startupFolder 'BIPASSION Local Admin.lnk'
$localLinks = @('Start Admin.lnk', 'Stop Admin.lnk', 'Open Admin.url')

if ($Uninstall) {
    if (Test-Path -LiteralPath $startupLink) { Remove-Item -LiteralPath $startupLink }
    $nodePath = (Get-Command node.exe -ErrorAction Stop).Source
    & $nodePath (Join-Path $launcherFolder 'local-admin.mjs') --stop
    foreach ($name in $localLinks) {
        $linkPath = Join-Path $launcherFolder $name
        if (Test-Path -LiteralPath $linkPath) { Remove-Item -LiteralPath $linkPath }
    }
    Write-Output 'Automatic startup removed.'
    exit 0
}

$nodePath = (Get-Command node.exe -ErrorAction Stop).Source
$wscriptPath = Join-Path $env:WINDIR 'System32\wscript.exe'
$scriptPath = Join-Path $launcherFolder 'local-admin.vbs'
$projectFolder = Split-Path $launcherFolder -Parent
foreach ($relative in @('backend\dist\server.js', 'admin-web\dist\index.html', 'admin-web\node_modules\vite\dist\node\index.js')) {
    if (-not (Test-Path -LiteralPath (Join-Path $projectFolder $relative))) {
        throw "Missing build/dependency: $relative. Build the project first."
    }
}
if (-not (Test-Path -LiteralPath $wscriptPath)) { throw 'Windows Script Host is unavailable.' }

$shell = New-Object -ComObject WScript.Shell
function Set-AdminShortcut([string]$destination, [string]$action) {
    $shortcut = $shell.CreateShortcut($destination)
    $shortcut.TargetPath = $wscriptPath
    $shortcut.Arguments = '"' + $scriptPath + '" ' + $action + ' "' + $nodePath + '"'
    $shortcut.WorkingDirectory = $projectFolder
    $shortcut.WindowStyle = 7
    $shortcut.Description = 'BIPASSION local Admin background launcher'
    $shortcut.Save()
}

Set-AdminShortcut $startupLink '--run'
Set-AdminShortcut (Join-Path $launcherFolder 'Start Admin.lnk') '--run'
Set-AdminShortcut (Join-Path $launcherFolder 'Stop Admin.lnk') '--stop'
Set-Content -LiteralPath (Join-Path $launcherFolder 'Open Admin.url') -Value "[InternetShortcut]`r`nURL=http://localhost:5173" -Encoding ascii
Start-Process -FilePath $wscriptPath -ArgumentList ('"' + $scriptPath + '" --run "' + $nodePath + '"') -WorkingDirectory $projectFolder -WindowStyle Hidden
Write-Output "Installed startup shortcut: $startupLink"
Write-Output 'Admin URL: http://localhost:5173'
