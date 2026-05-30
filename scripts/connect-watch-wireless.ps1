param(
    [string]$PairAddress,
    [string]$PairCode,
    [string]$ConnectAddress,
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

function Get-WatchSerial {
    $devices = Get-ConnectedDevices
    foreach ($serial in $devices) {
        if (Test-WatchDevice -Serial $serial) {
            return $serial
        }
    }
    return $null
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

$existingWatchSerial = Get-WatchSerial
if (-not [string]::IsNullOrWhiteSpace($existingWatchSerial)) {
    Write-Host "Watch already connected: $existingWatchSerial"
    exit 0
}

$endpoints = @(Get-MdnsEndpoints)
if ($endpoints.Count -gt 0) {
    Write-Host "Discovered wireless debugging services:"
    $endpoints | ForEach-Object { Write-Host "  $($_.Service)  $($_.Address)" }
}

if ([string]::IsNullOrWhiteSpace($PairAddress)) {
    $pairCandidates = @($endpoints | Where-Object { $_.Service -eq "_adb-tls-pairing._tcp" } | Select-Object -ExpandProperty Address -Unique)
    if ($pairCandidates.Count -eq 1) {
        $PairAddress = $pairCandidates[0]
        Write-Host "Auto-selected pairing address: $PairAddress"
    }
}

if (-not [string]::IsNullOrWhiteSpace($PairCode)) {
    if ([string]::IsNullOrWhiteSpace($PairAddress)) {
        throw "PairCode was provided, but PairAddress is missing. Open the watch pairing-code screen or pass -PairAddress WATCH_IP:PAIR_PORT."
    }
    Write-Host "Pairing watch at $PairAddress"
    Invoke-Adb pair $PairAddress $PairCode
}

if ([string]::IsNullOrWhiteSpace($ConnectAddress)) {
    $connectCandidates = @($endpoints | Where-Object { $_.Service -eq "_adb-tls-connect._tcp" } | Select-Object -ExpandProperty Address -Unique)
    if ($connectCandidates.Count -eq 1) {
        $ConnectAddress = $connectCandidates[0]
        Write-Host "Auto-selected connect address: $ConnectAddress"
    }
}

if ([string]::IsNullOrWhiteSpace($ConnectAddress)) {
    throw @"
Could not find a unique watch connect address.

On the watch:
  Settings > Developer options > ADB debugging: On
  Settings > Developer options > Wireless debugging: On

Then either run this again, or pass:
  .\scripts\connect-watch-wireless.ps1 -ConnectAddress WATCH_IP:CONNECT_PORT
"@
}

Write-Host "Connecting watch at $ConnectAddress"
$connectOutput = Invoke-Adb connect $ConnectAddress
$connectOutput | ForEach-Object { Write-Host $_ }
if (($connectOutput -join "`n") -match "(?i)failed|unable|cannot|refused|timed out") {
    throw @"
Could not connect to $ConnectAddress.

If this is the first connection from this PC, open on the watch:
  Wireless debugging > Pair new device with pairing code

Then run:
  .\scripts\connect-watch-wireless.ps1 -PairAddress WATCH_IP:PAIR_PORT -PairCode PAIR_CODE
"@
}

$watchSerial = Get-WatchSerial
if ([string]::IsNullOrWhiteSpace($watchSerial)) {
    throw @"
Connected to $ConnectAddress, but no watch device is available yet.

If this is the first connection from this PC, open on the watch:
  Wireless debugging > Pair new device with pairing code

Then run:
  .\scripts\connect-watch-wireless.ps1 -PairAddress WATCH_IP:PAIR_PORT -PairCode PAIR_CODE
"@
}

Write-Host "Watch connected: $watchSerial"
