# Removes the apps in apps-to-remove.txt for the TV's user. Without -Apply it only shows what it would do.
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [switch]$Apply
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
$device = Connect-Tv $adb $Tv

$installed = & $adb -s $device shell 'pm list packages --user 0'
$found = @(Read-AppList "$PSScriptRoot\apps-to-remove.txt" | Where-Object { $installed -contains "package:$_" })

if ($found.Count -eq 0) {
    'None of the listed apps are on this TV.'
    return
}
if (-not $Apply) {
    "Would remove $($found.Count) apps:"
    $found | ForEach-Object { "  $_" }
    ''
    'Nothing was changed. To remove them, add -Apply.'
    return
}
foreach ($app in $found) {
    $result = "$(& $adb -s $device shell "pm uninstall --user 0 $app")"
    "$app : $result"
}
''
'Done. To bring them back, run restore.ps1.'
