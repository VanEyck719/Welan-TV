# Functions shared by the other scripts.

function Get-Adb {
    $found = Get-Command adb -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    $paths = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe"
    )
    $winget = Get-ChildItem "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Google.PlatformTools*\platform-tools\adb.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
    if ($winget) { $paths += $winget }
    foreach ($path in $paths) { if (Test-Path $path) { return $path } }
    throw 'ADB not found. Install Android Platform Tools.'
}

# Returns the address of the connected TV. The first time, the prompt on the TV has to be accepted.
function Connect-Tv($adb, $tv) {
    $address = if ($tv -match ':') { $tv } else { "${tv}:5555" }
    for ($i = 0; $i -lt 20; $i++) {
        & $adb connect $address | Out-Null
        if ((& $adb devices) -match "$([regex]::Escape($address))\s+device") { return $address }
        if ($i -eq 0) { Write-Host 'Waiting for the connection. If a prompt appears on the TV, tick "Always allow" and accept.' }
        Start-Sleep 3
    }
    throw "Could not connect to $address. Check the IP address and that USB debugging is on."
}

function Read-AppList($path) {
    Get-Content $path | ForEach-Object { $_.Trim() } | Where-Object { $_ -and -not $_.StartsWith('#') }
}
