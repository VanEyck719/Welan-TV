# Removes Inicio and the screensaver, leaving the TV on Google's launcher.
param([Parameter(Mandatory = $true)][string]$Tv)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
$device = Connect-Tv $adb $Tv
function sh($command) { & $adb -s $device shell $command }

$service = 'tv.inicio.launcher/tv.inicio.launcher.HomeGuard'
$enabled = "$(sh 'settings get secure enabled_accessibility_services')".Trim()
$others = @($enabled -split ':' | Where-Object { $_ -and $_ -ne 'null' -and $_ -ne $service })
sh "settings put secure enabled_accessibility_services '$($others -join ':')'"

if ("$(sh 'settings get secure screensaver_components')" -match 'tv\.inicio\.protector') {
    sh 'settings delete secure screensaver_components'
}
$installed = sh 'pm list packages tv.inicio'
foreach ($package in 'tv.inicio.launcher', 'tv.inicio.protector') {
    if ($installed -contains "package:$package") { "$package : $(& $adb -s $device uninstall $package)" }
}
sh 'input keyevent KEYCODE_HOME' | Out-Null
'Done. Copied pictures and videos are still in /sdcard/Pictures/Fondos and /sdcard/Movies/Protector.'
