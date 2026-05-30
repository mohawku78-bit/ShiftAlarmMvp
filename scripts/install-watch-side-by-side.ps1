param(
    [string]$PhoneSerial,
    [string]$WatchSerial,
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$JavaHome = "C:\Program Files\Android\Android Studio1\jbr",
    [switch]$SkipBuild,
    [switch]$SkipPreflight,
    [switch]$SkipVerify,
    [switch]$SkipLaunch
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$PackageName = "com.example.shiftalarmmvp.next"
$PhoneApk = Join-Path $RootDir "app\build\outputs\apk\sideBySide\app-sideBySide.apk"
$WatchApk = Join-Path $RootDir "wear\build\outputs\apk\sideBySide\wear-sideBySide.apk"

function Invoke-Adb {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )
    & $AdbPath @Args
}

function Assert-File {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Required file not found: $Path"
    }
}

function Invoke-Gradle {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )

    $previousJavaHome = $env:JAVA_HOME
    if (-not [string]::IsNullOrWhiteSpace($JavaHome) -and (Test-Path -LiteralPath $JavaHome)) {
        $env:JAVA_HOME = $JavaHome
    }

    try {
        & .\gradlew.bat @Args
    } finally {
        $env:JAVA_HOME = $previousJavaHome
    }
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

Push-Location $RootDir
try {
    if (-not $SkipBuild) {
        Invoke-Gradle :app:assembleSideBySide :wear:assembleSideBySide
    }

    Assert-File $PhoneApk
    Assert-File $WatchApk

    if (-not $SkipPreflight) {
        Write-Host ""
        Write-Host "Running watch integration source preflight"
        & powershell -ExecutionPolicy Bypass -File .\scripts\validate-watch-integration-source.ps1
    }

    Write-Host ""
    Write-Host "Connected devices:"
    Invoke-Adb devices -l

    if ([string]::IsNullOrWhiteSpace($PhoneSerial) -or [string]::IsNullOrWhiteSpace($WatchSerial)) {
        Write-Host ""
        Write-Host "Usage:"
        Write-Host "  .\scripts\install-watch-side-by-side.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL"
        Write-Host ""
        Write-Host "Use the serials shown above. For a Galaxy Watch over wireless debugging, run adb connect first."
        exit 2
    }

    Write-Host ""
    Write-Host "Installing phone APK to $PhoneSerial"
    Invoke-Adb -s $PhoneSerial install -r $PhoneApk

    Write-Host ""
    Write-Host "Installing watch APK to $WatchSerial"
    Invoke-Adb -s $WatchSerial install -r $WatchApk

    Write-Host ""
    Write-Host "Granting notification permission when available"
    Invoke-Adb -s $PhoneSerial shell pm grant $PackageName android.permission.POST_NOTIFICATIONS 2>$null
    Invoke-Adb -s $WatchSerial shell pm grant $PackageName android.permission.POST_NOTIFICATIONS 2>$null

    if (-not $SkipVerify) {
        Write-Host ""
        Write-Host "Verifying installed phone/watch packages"
        $verifyArgs = @(
            "-ExecutionPolicy", "Bypass",
            "-File", ".\scripts\verify-watch-side-by-side.ps1",
            "-PhoneSerial", $PhoneSerial,
            "-WatchSerial", $WatchSerial,
            "-AdbPath", $AdbPath
        )
        if (-not $SkipLaunch) {
            $verifyArgs += "-LaunchApps"
        }
        & powershell @verifyArgs
    } elseif (-not $SkipLaunch) {
        Write-Host ""
        Write-Host "Launching phone and watch apps"
        Invoke-Adb -s $PhoneSerial shell monkey -p $PackageName 1
        Invoke-Adb -s $WatchSerial shell monkey -p $PackageName 1
    }

    Write-Host ""
    Write-Host "Install complete. Open the phone test area and tap '워치 알람 미리보기 보내기'."
} finally {
    Pop-Location
}
