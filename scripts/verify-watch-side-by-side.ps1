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

function Show-PackageSummary {
    param(
        [string]$Serial,
        [string]$Label
    )

    $dump = Invoke-Adb -s $Serial shell dumpsys package $PackageName
    $summary = $dump | Select-String -Pattern "versionCode|versionName|android.permission.POST_NOTIFICATIONS|granted=true"

    Write-Host ""
    Write-Host "$Label package summary:"
    if ($summary) {
        $summary | ForEach-Object { Write-Host $_.Line.Trim() }
    } else {
        Write-Host "No package summary lines found."
    }
}

function Show-WatchFeatureCheck {
    param([string]$Serial)

    $features = Invoke-Adb -s $Serial shell pm list features
    $hasWatchFeature = $features | Select-String -Pattern "android.hardware.type.watch"

    Write-Host ""
    if ($hasWatchFeature) {
        Write-Host "Watch feature check: OK"
    } else {
        Write-Host "Watch feature check: not reported. Confirm this serial is the Galaxy Watch."
    }
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

Write-Host "Connected devices:"
Invoke-Adb devices -l

Assert-Device -Serial $PhoneSerial -Label "Phone"
Assert-Device -Serial $WatchSerial -Label "Watch"

Test-PackageInstalled -Serial $PhoneSerial -Label "Phone"
Test-PackageInstalled -Serial $WatchSerial -Label "Watch"
Show-PackageSummary -Serial $PhoneSerial -Label "Phone"
Show-PackageSummary -Serial $WatchSerial -Label "Watch"
Show-WatchFeatureCheck -Serial $WatchSerial

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
