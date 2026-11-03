# Installs Inicio as the home screen and, if asked, the screensaver and your own files.
# Run it again after updating: it reconnects the service behind the Home button.
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [switch]$Screensaver,
    [string]$Wallpapers,
    [string]$Videos,
    [int]$ScreensaverMinutes = 0
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
$root = Split-Path -Parent $PSScriptRoot
foreach ($apk in 'Inicio', 'Protector') {
    if (-not (Test-Path "$root\apk\$apk.apk")) { throw "apk\$apk.apk is missing. Run build.ps1 first." }
}
$adb = Get-Adb
$device = Connect-Tv $adb $Tv
function sh($command) { & $adb -s $device shell $command }

function Copy-Files($folder, $target, $extensions) {
    sh "mkdir -p $target"
    $files = @(Get-ChildItem $folder -File | Where-Object { $extensions -contains $_.Extension.ToLower() })
    foreach ($file in $files) { & $adb -s $device push $file.FullName "$target/$($file.Name)" | Out-Null }
    "Copied $($files.Count) files to $target"
}

# TCL TVs don't let sideloaded apps start on their own unless this is allowed.
function Allow-Start($package) {
    sh "pm grant $package android.permission.READ_EXTERNAL_STORAGE"
    sh "appops set $package APP_AUTO_START allow; appops set $package APP_ASSOC_START allow"
}

'Installing Inicio...'
& $adb -s $device install -r "$root\apk\Inicio.apk" | Out-Null
Allow-Start 'tv.inicio.launcher'
if ($Wallpapers) { Copy-Files $Wallpapers '/sdcard/Pictures/Fondos' @('.jpg', '.jpeg', '.png', '.webp') }

# Keeps other accessibility services. It has to go off and on again to reconnect.
$service = 'tv.inicio.launcher/tv.inicio.launcher.HomeGuard'
$enabled = "$(sh 'settings get secure enabled_accessibility_services')".Trim()
$others = @($enabled -split ':' | Where-Object { $_ -and $_ -ne 'null' -and $_ -ne $service })
sh "settings put secure enabled_accessibility_services '$($others -join ':')'"
Start-Sleep 2
sh "settings put secure enabled_accessibility_services '$(($others + $service) -join ':')'"
sh 'settings put secure accessibility_enabled 1'
sh 'cmd package set-home-activity --user 0 tv.inicio.launcher/.InicioActivity' | Out-Null

if ($Screensaver) {
    'Installing the screensaver...'
    & $adb -s $device install -r "$root\apk\Protector.apk" | Out-Null
    Allow-Start 'tv.inicio.protector'
    sh 'settings put secure screensaver_components tv.inicio.protector/.ProtectorDream'
    sh 'settings put secure screensaver_enabled 1'
    if ($ScreensaverMinutes -gt 0) { sh "settings put system screen_off_timeout $($ScreensaverMinutes * 60000)" }
    if ($Videos) { Copy-Files $Videos '/sdcard/Movies/Protector' @('.mp4', '.mkv', '.webm') }
}

sh 'input keyevent KEYCODE_HOME' | Out-Null
'Done. Press Home on the remote to check.'
