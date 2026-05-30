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

function Get-ConnectedDevices {
    $lines = Invoke-Adb devices -l
    $serials = @()
    foreach ($line in $lines) {
        if ($line -match "^(\S+)\s+device\b") {
            $serials += $Matches[1]
        }
    }
    return $serials
}

function Test-WatchDevice {
    param([string]$Serial)

    $features = Invoke-Adb -s $Serial shell pm list features
    return ($features | Select-String -Pattern "android.hardware.type.watch" -Quiet) -eq $true
}

function Resolve-DeviceSerials {
    if (-not [string]::IsNullOrWhiteSpace($PhoneSerial) -and -not [string]::IsNullOrWhiteSpace($WatchSerial)) {
        return
    }

    $devices = Get-ConnectedDevices
    if ($devices.Count -eq 0) {
        throw "No adb devices are connected. Connect the phone with USB debugging and the Galaxy Watch with wireless debugging, then run adb devices -l."
    }

    $classified = foreach ($serial in $devices) {
        [pscustomobject]@{
            Serial = $serial
            IsWatch = Test-WatchDevice -Serial $serial
        }
    }

    if ([string]::IsNullOrWhiteSpace($WatchSerial)) {
        $watchCandidates = @($classified | Where-Object { $_.IsWatch })
        if ($watchCandidates.Count -eq 1) {
            $script:WatchSerial = $watchCandidates[0].Serial
            Write-Host "Auto-selected watch serial: $WatchSerial"
        }
    }

    if ([string]::IsNullOrWhiteSpace($PhoneSerial)) {
        $phoneCandidates = @($classified | Where-Object { -not $_.IsWatch })
        if ($phoneCandidates.Count -eq 1) {
            $script:PhoneSerial = $phoneCandidates[0].Serial
            Write-Host "Auto-selected phone serial: $PhoneSerial"
        }
    }

    if ([string]::IsNullOrWhiteSpace($PhoneSerial) -or [string]::IsNullOrWhiteSpace($WatchSerial)) {
        $table = ($classified | ForEach-Object {
            $kind = if ($_.IsWatch) { "watch" } else { "phone_or_other" }
            "  $($_.Serial)  $kind"
        }) -join [Environment]::NewLine

        throw @"
Could not uniquely resolve phone/watch serials.
Detected:
$table

Run:
  .\scripts\install-watch-side-by-side.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL

For Galaxy Watch wireless debugging, connect it first with:
  .\scripts\connect-watch-wireless.ps1
"@
    }
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
    Resolve-DeviceSerials

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
    Write-Host "Install complete. Run run-watch-full-validation.ps1, or open the phone test area and send a watch preview."
} finally {
    Pop-Location
}
