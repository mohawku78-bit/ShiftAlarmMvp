param(
    [string]$PhoneSerial,
    [string]$WatchSerial,
    [string]$PackageName = "com.example.shiftalarmmvp.next",
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [switch]$LaunchApps
)

$ErrorActionPreference = "Stop"

function Invoke-Adb {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )
    & $AdbPath @Args
}

function Assert-Device {
    param(
        [string]$Serial,
        [string]$Label
    )

    if ([string]::IsNullOrWhiteSpace($Serial)) {
        throw "$Label serial is required."
    }

    $state = (Invoke-Adb -s $Serial get-state) -join ""
    if ($state.Trim() -ne "device") {
        throw "$Label is not ready through adb: $Serial ($state)"
    }
}

function Test-PackageInstalled {
    param(
        [string]$Serial,
        [string]$Label
    )

    $packagePath = (Invoke-Adb -s $Serial shell pm path $PackageName) -join "`n"
    if ($packagePath -notmatch "^package:") {
        throw "$PackageName is not installed on $Label ($Serial)."
    }

    Write-Host ""
    Write-Host "$Label package:"
    Write-Host $packagePath
}

function Get-DeviceSdkInt {
    param([string]$Serial)

    $raw = (Invoke-Adb -s $Serial shell getprop ro.build.version.sdk) -join ""
    $value = $raw.Trim()
    $sdk = 0
    if (-not [int]::TryParse($value, [ref]$sdk)) {
        throw "Could not read Android SDK level from ${Serial}: $value"
    }
    return $sdk
}

function Get-DeviceProp {
    param(
        [string]$Serial,
        [string]$Name
    )

    return ((Invoke-Adb -s $Serial shell getprop $Name) -join "").Trim()
}

function Get-PackageDump {
    param([string]$Serial)

    return @(Invoke-Adb -s $Serial shell dumpsys package $PackageName)
}

function Get-PackageVersionName {
    param([string[]]$Dump)

    foreach ($line in $Dump) {
        if ($line -match "^\s*versionName=(.+)\s*$") {
            return $Matches[1].Trim()
        }
    }
    return ""
}

function Assert-SideBySideVersion {
    param(
        [string[]]$Dump,
        [string]$Label
    )

    if (-not $PackageName.EndsWith(".next")) {
        return
    }

    $versionName = Get-PackageVersionName -Dump $Dump
    if ([string]::IsNullOrWhiteSpace($versionName)) {
        throw "Could not verify $Label versionName for $PackageName."
    }
    if ($versionName -notmatch "-next$") {
        throw "$Label package is installed, but it does not look like the side-by-side build: versionName=$versionName"
    }
}

function Show-PackageSummary {
    param(
        [string[]]$Dump,
        [string]$Label
    )

    $summary = $Dump | Select-String -Pattern "versionCode|versionName|android.permission.POST_NOTIFICATIONS"

    Write-Host ""
    Write-Host "$Label package summary:"
    if ($summary) {
        $summary | ForEach-Object { Write-Host $_.Line.Trim() }
    } else {
        Write-Host "No package summary lines found."
    }
}

function Assert-DeviceKind {
    param(
        [string]$Serial,
        [string]$Label,
        [bool]$ShouldBeWatch
    )

    $features = Invoke-Adb -s $Serial shell pm list features
    $hasWatchFeature = ($features | Select-String -Pattern "android.hardware.type.watch" -Quiet) -eq $true

    Write-Host ""
    if ($ShouldBeWatch) {
        if (-not $hasWatchFeature) {
            throw "$Label does not report android.hardware.type.watch. Check the WatchSerial value: $Serial"
        }
        Write-Host "$Label watch feature check: OK"
        return
    }

    if ($hasWatchFeature) {
        throw "$Label reports android.hardware.type.watch. Check the PhoneSerial value: $Serial"
    }
    Write-Host "$Label phone feature check: OK"
}

function Assert-NotificationPermission {
    param(
        [string[]]$Dump,
        [string]$Label,
        [int]$SdkInt
    )

    if ($SdkInt -lt 33) {
        Write-Host "$Label notification permission: not required on SDK $SdkInt"
        return
    }

    $match = $Dump |
        Select-String -Pattern "android\.permission\.POST_NOTIFICATIONS:.*granted=(true|false)" |
        Select-Object -First 1

    if (-not $match) {
        throw "Could not verify $Label POST_NOTIFICATIONS permission on SDK $SdkInt."
    }

    $granted = $match.Matches[0].Groups[1].Value -eq "true"
    if (-not $granted) {
        throw "$Label POST_NOTIFICATIONS permission is not granted: $($match.Line.Trim())"
    }

    Write-Host "$Label notification permission: OK"
}

function Show-DeviceSummary {
    param(
        [string]$Serial,
        [string]$Label,
        [int]$SdkInt
    )

    $manufacturer = Get-DeviceProp -Serial $Serial -Name "ro.product.manufacturer"
    $model = Get-DeviceProp -Serial $Serial -Name "ro.product.model"
    Write-Host ""
    Write-Host "$Label device: $manufacturer $model (SDK $SdkInt, serial $Serial)"
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

Write-Host "Connected devices:"
Invoke-Adb devices -l

Assert-Device -Serial $PhoneSerial -Label "Phone"
Assert-Device -Serial $WatchSerial -Label "Watch"

$phoneSdk = Get-DeviceSdkInt -Serial $PhoneSerial
$watchSdk = Get-DeviceSdkInt -Serial $WatchSerial
Show-DeviceSummary -Serial $PhoneSerial -Label "Phone" -SdkInt $phoneSdk
Show-DeviceSummary -Serial $WatchSerial -Label "Watch" -SdkInt $watchSdk
Assert-DeviceKind -Serial $PhoneSerial -Label "Phone" -ShouldBeWatch $false
Assert-DeviceKind -Serial $WatchSerial -Label "Watch" -ShouldBeWatch $true

Test-PackageInstalled -Serial $PhoneSerial -Label "Phone"
Test-PackageInstalled -Serial $WatchSerial -Label "Watch"

$phoneDump = Get-PackageDump -Serial $PhoneSerial
$watchDump = Get-PackageDump -Serial $WatchSerial
Assert-SideBySideVersion -Dump $phoneDump -Label "Phone"
Assert-SideBySideVersion -Dump $watchDump -Label "Watch"
Show-PackageSummary -Dump $phoneDump -Label "Phone"
Show-PackageSummary -Dump $watchDump -Label "Watch"
Assert-NotificationPermission -Dump $phoneDump -Label "Phone" -SdkInt $phoneSdk
Assert-NotificationPermission -Dump $watchDump -Label "Watch" -SdkInt $watchSdk

if ($LaunchApps) {
    Write-Host ""
    Write-Host "Launching phone and watch apps."
    Invoke-Adb -s $PhoneSerial shell monkey -p $PackageName 1
    Invoke-Adb -s $WatchSerial shell monkey -p $PackageName 1
}

Write-Host ""
Write-Host "Verification checks complete."
Write-Host "Next UI path: open the phone test area, send watch preview, then run the watch stop/snooze control test."
Write-Host "Next ADB path: .\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial $PhoneSerial -WatchSerial $WatchSerial -Mode preview -Clear -Assert"
Write-Host "Automated control path: .\scripts\run-watch-side-by-side-smoke.ps1 -PhoneSerial $PhoneSerial -WatchSerial $WatchSerial -Mode control -AutoWatchAction stop -Clear -Assert"
Write-Host "Full validation path: .\scripts\run-watch-full-validation.ps1 -PhoneSerial $PhoneSerial -WatchSerial $WatchSerial -SkipBuild -SkipInstall"
