# Builds Inicio.apk and Protector.apk without Gradle: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Needs the Android SDK (build-tools 34.0.0 and platform android-30) and a JDK 17.
param(
    [string]$Sdk = $(if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }),
    [string]$Jdk = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$bt   = "$Sdk\build-tools\34.0.0"
$jar  = "$Sdk\platforms\android-30\android.jar"
if (-not $Jdk) {
    $Jdk = Get-ChildItem "C:\Program Files\Microsoft" -Directory -Filter "jdk-17*" -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not (Test-Path "$bt\aapt2.exe")) { throw "build-tools 34.0.0 not found in $Sdk" }
if (-not (Test-Path $jar)) { throw "Platform android-30 not found in $Sdk" }
if (-not $Jdk -or -not (Test-Path "$Jdk\bin\javac.exe")) { throw "JDK 17 not found. Set it with -Jdk or JAVA_HOME." }
$env:JAVA_HOME = $Jdk

# The key is created once on this PC and is never uploaded (see .gitignore).
$keystore = "$root\local.keystore"
if (-not (Test-Path $keystore)) {
    $ErrorActionPreference = 'Continue'
    & "$Jdk\bin\keytool.exe" -genkeypair -keystore $keystore -storepass android -keypass android `
        -alias local -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Local" 2>&1 | Out-Null
    $ErrorActionPreference = 'Stop'
}

function Build-App($name, $dir, [string[]]$shared) {
    $out = "$dir\build"
    if (Test-Path $out) { Remove-Item -Recurse -Force $out }
    New-Item -ItemType Directory -Force "$out\gen", "$out\classes", "$out\dex" | Out-Null

    & "$bt\aapt2.exe" compile --dir "$dir\res" -o "$out\res.zip"
    & "$bt\aapt2.exe" link -o "$out\unsigned.apk" -I $jar --manifest "$dir\AndroidManifest.xml" --java "$out\gen" "$out\res.zip"

    $sources = @(Get-ChildItem "$dir\src", "$out\gen" -Recurse -Filter *.java | ForEach-Object { $_.FullName }) + $shared
    $ErrorActionPreference = 'Continue'
    & "$Jdk\bin\javac.exe" --release 8 -encoding UTF-8 -nowarn -classpath $jar -d "$out\classes" $sources 2>&1 |
        ForEach-Object { "$_" } | Where-Object { $_ -notmatch '^Note:|warning: \[options\]|^\d+ warnings?$' }
    if ($LASTEXITCODE -ne 0) { throw "javac failed for $name" }
    $ErrorActionPreference = 'Stop'
    $classes = Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object { $_.FullName }
    & "$bt\d8.bat" --min-api 21 --lib $jar --output "$out\dex" $classes

    Copy-Item "$out\dex\classes.dex" "$out\classes.dex"
    Push-Location $out
    & "$Jdk\bin\jar.exe" uf unsigned.apk classes.dex
    Pop-Location
    & "$bt\zipalign.exe" -f 4 "$out\unsigned.apk" "$out\aligned.apk"
    New-Item -ItemType Directory -Force "$root\apk" | Out-Null
    & "$bt\apksigner.bat" sign --ks $keystore --ks-pass pass:android --key-pass pass:android `
        --out "$root\apk\$name.apk" "$out\aligned.apk"
    "OK: apk\$name.apk ($([math]::Round((Get-Item "$root\apk\$name.apk").Length / 1KB)) KB)"
}

# The screensaver shares the pet and the weather with the launcher.
$from = "$root\launcher\src\tv\inicio\launcher"
Build-App 'Inicio' "$root\launcher" @()
Build-App 'Protector' "$root\protector" @("$from\Bichito.java", "$from\ClimaView.java", "$from\Ui.java")
