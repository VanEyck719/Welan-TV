# Brings back any apps in apps-to-remove.txt that were removed.
param([Parameter(Mandatory = $true)][string]$Tv)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
$device = Connect-Tv $adb $Tv

$installed = & $adb -s $device shell 'pm list packages --user 0'
foreach ($app in (Read-AppList "$PSScriptRoot\apps-to-remove.txt")) {
    if ($installed -contains "package:$app") { continue }
    $result = "$(& $adb -s $device shell "cmd package install-existing $app")"
    "$app : $result"
}
'Done.'
