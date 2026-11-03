# Sets animations to 0.5x and AdGuard's DNS. -Undo goes back to the normal values.
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [switch]$Undo
)
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\common.ps1"
$adb = Get-Adb
$device = Connect-Tv $adb $Tv
function sh($command) { & $adb -s $device shell $command }

$scale = if ($Undo) { '1.0' } else { '0.5' }
foreach ($name in 'window_animation_scale', 'transition_animation_scale', 'animator_duration_scale') {
    sh "settings put global $name $scale"
}
"Animations: ${scale}x"

if ($Undo) {
    sh 'settings put global private_dns_mode opportunistic'
    sh 'settings delete global private_dns_specifier' | Out-Null
    'DNS: automatic'
} else {
    sh 'settings put global private_dns_mode hostname'
    sh 'settings put global private_dns_specifier dns.adguard-dns.com'
    'DNS: AdGuard. If an app misbehaves, try -Undo.'
}
