param(
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

$ErrorActionPreference = "Stop"

function Invoke-Adb {
    param(
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Args
    )
    & $AdbPath @Args
}

function Get-ConnectedDevices {
    $lines = Invoke-Adb devices -l
    foreach ($line in $lines) {
        if ($line -match "^(\S+)\s+device\b") {
            $serial = $Matches[1]
            [pscustomobject]@{
                Serial = $serial
                Raw = $line.Trim()
            }
        }
    }
}

function Test-WatchDevice {
    param([string]$Serial)

    $features = Invoke-Adb -s $Serial shell pm list features
    return ($features | Select-String -Pattern "android.hardware.type.watch" -Quiet) -eq $true
}

function Get-MdnsEndpoints {
    $lines = Invoke-Adb mdns services
    foreach ($line in $lines) {
        if ($line -match "\s(_adb-tls-(pairing|connect)\._tcp)\s+(\S+:\d+)") {
            [pscustomobject]@{
                Service = $Matches[1]
                Address = $Matches[3]
            }
        }
    }
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

Write-Host "ADB path: $AdbPath"
Write-Host ""
Write-Host "Connected devices:"
Invoke-Adb devices -l

$devices = @(Get-ConnectedDevices)
$classified = @()
foreach ($device in $devices) {
    $isWatch = Test-WatchDevice -Serial $device.Serial
    $classified += [pscustomobject]@{
        Serial = $device.Serial
        Kind = if ($isWatch) { "watch" } else { "phone_or_other" }
        Raw = $device.Raw
    }
}

Write-Host ""
if ($classified.Count -eq 0) {
    Write-Host "No ready ADB devices were detected."
} else {
    Write-Host "Detected device roles:"
    $classified | ForEach-Object {
        Write-Host "  $($_.Serial)  $($_.Kind)"
    }
}

Write-Host ""
Write-Host "Wireless debugging discovery:"
$endpoints = @(Get-MdnsEndpoints)
if ($endpoints.Count -eq 0) {
    Write-Host "  No _adb-tls pairing/connect services discovered."
} else {
    $endpoints | ForEach-Object {
        Write-Host "  $($_.Service)  $($_.Address)"
    }
}

$phoneCount = @($classified | Where-Object { $_.Kind -eq "phone_or_other" }).Count
$watchCount = @($classified | Where-Object { $_.Kind -eq "watch" }).Count
$pairingCount = @($endpoints | Where-Object { $_.Service -eq "_adb-tls-pairing._tcp" }).Count
$connectCount = @($endpoints | Where-Object { $_.Service -eq "_adb-tls-connect._tcp" }).Count

Write-Host ""
Write-Host "Next steps:"
if ($phoneCount -eq 0) {
    Write-Host "  1. Connect the phone over USB, enable USB debugging, and accept the RSA prompt."
} else {
    Write-Host "  1. Phone ADB device is visible."
}

if ($watchCount -eq 0) {
    Write-Host "  2. On the watch, enable Developer options > ADB debugging and Wireless debugging."
    if ($pairingCount -gt 0) {
        Write-Host "  3. Pair the watch:"
        Write-Host "     .\scripts\connect-watch-wireless.ps1 -PairAddress WATCH_IP:PAIR_PORT -PairCode PAIR_CODE"
    } elseif ($connectCount -gt 0) {
        Write-Host "  3. Connect the watch:"
        Write-Host "     .\scripts\connect-watch-wireless.ps1"
    } else {
        Write-Host "  3. Open the watch Wireless debugging screen and keep it awake near this PC, then rerun this script."
    }
} else {
    Write-Host "  2. Watch ADB device is visible."
}

if ($phoneCount -gt 0 -and $watchCount -gt 0) {
    Write-Host "  3. Run full validation:"
    Write-Host "     .\scripts\run-watch-full-validation.ps1"
} else {
    Write-Host "  4. After both devices appear, run:"
    Write-Host "     .\scripts\run-watch-full-validation.ps1"
}
