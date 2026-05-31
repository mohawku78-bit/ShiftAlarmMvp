param(
    [string]$PhoneSerial,
    [string]$WatchSerial,
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$JavaHome = "C:\Program Files\Android\Android Studio1\jbr",
    [string]$OutputDir = "manual-validation\watch-alarm",
    [int]$PreviewWaitSeconds = 20,
    [int]$ControlWaitSeconds = 20,
    [int]$AutoWatchActionDelaySeconds = 4,
    [int]$AutoWatchActionAttempts = 3,
    [int]$AutoWatchActionRetrySeconds = 2,
    [string]$AutoWatchStopKeyCode = "KEYCODE_STEM_PRIMARY",
    [string]$AutoWatchSnoozeKeyCode = "KEYCODE_BACK",
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$SkipLaunch,
    [switch]$SkipOrphanSmoke,
    [switch]$SkipFallbackSmoke,
    [switch]$SkipHardwareKeySmoke
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$ResolvedOutputDir = Join-Path $RootDir $OutputDir
$StepResults = New-Object System.Collections.Generic.List[object]
$FailedStep = $null

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
    Write-Host "Connected devices:"
    Invoke-Adb devices -l

    if (-not [string]::IsNullOrWhiteSpace($PhoneSerial) -and -not [string]::IsNullOrWhiteSpace($WatchSerial)) {
        return
    }

    $devices = Get-ConnectedDevices
    if ($devices.Count -eq 0) {
        throw @"
No adb devices are connected.

Run:
  .\scripts\diagnose-watch-adb.ps1

Then connect the phone with USB debugging and the Galaxy Watch with wireless debugging.
"@
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
  .\scripts\run-watch-full-validation.ps1 -PhoneSerial PHONE_SERIAL -WatchSerial WATCH_SERIAL

For Galaxy Watch wireless debugging, connect it first with:
  adb connect WATCH_IP:WATCH_PORT

To diagnose missing devices first, run:
  .\scripts\diagnose-watch-adb.ps1
"@
    }
}

function Invoke-ValidationStep {
    param(
        [string]$Name,
        [scriptblock]$Block
    )

    $startedAt = Get-Date
    Write-Host ""
    Write-Host "== $Name =="
    try {
        & $Block
        $StepResults.Add([pscustomobject]@{
            Step = $Name
            Status = "PASS"
            StartedAt = $startedAt
            EndedAt = Get-Date
            Error = ""
        })
    } catch {
        $script:FailedStep = $Name
        $StepResults.Add([pscustomobject]@{
            Step = $Name
            Status = "FAIL"
            StartedAt = $startedAt
            EndedAt = Get-Date
            Error = $_.Exception.Message
        })
        throw
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

function Assert-LastExitCode {
    param([string]$StepName)

    if ($LASTEXITCODE -ne 0) {
        throw "$StepName failed with exit code $LASTEXITCODE"
    }
}

function Invoke-Smoke {
    param(
        [ValidateSet("preview", "stop", "snooze", "orphan")]
        [string]$Scenario,
        [ValidateSet("broadcast", "hardwareKey")]
        [string]$AutoWatchActionSource = "broadcast",
        [ValidateSet("any", "notification", "fallback")]
        [string]$ExpectedDisplayMode = "notification",
        [switch]$BlockWatchNotifications
    )

    $args = @(
        "-ExecutionPolicy", "Bypass",
        "-File", ".\scripts\run-watch-side-by-side-smoke.ps1",
        "-PhoneSerial", $PhoneSerial,
        "-WatchSerial", $WatchSerial,
        "-AdbPath", $AdbPath,
        "-OutputDir", $OutputDir,
        "-Clear",
        "-Assert"
    )

    if ($Scenario -eq "preview") {
        $args += @(
            "-Mode", "preview",
            "-Label", "Full validation preview",
            "-WaitSeconds", $PreviewWaitSeconds.ToString()
        )
    } else {
        $mode = if ($Scenario -eq "orphan") { "orphan" } else { $Scenario }
        $watchAction = if ($Scenario -eq "orphan") { "stop" } else { $Scenario }
        $args += @(
            "-Mode", $mode,
            "-Label", "Full validation $Scenario",
            "-AutoWatchActionSource", $AutoWatchActionSource,
            "-ExpectedAction", $watchAction,
            "-AutoWatchStopKeyCode", $AutoWatchStopKeyCode,
            "-AutoWatchSnoozeKeyCode", $AutoWatchSnoozeKeyCode,
            "-AutoWatchActionDelaySeconds", $AutoWatchActionDelaySeconds.ToString(),
            "-AutoWatchActionAttempts", $AutoWatchActionAttempts.ToString(),
            "-AutoWatchActionRetrySeconds", $AutoWatchActionRetrySeconds.ToString(),
            "-WaitSeconds", $ControlWaitSeconds.ToString()
        )
        if ($Scenario -eq "orphan") {
            $args += @("-AutoWatchAction", $watchAction)
        }
    }

    if ($BlockWatchNotifications) {
        $args += "-BlockWatchNotifications"
    }
    $args += @("-ExpectedDisplayMode", $ExpectedDisplayMode)

    & powershell @args
    Assert-LastExitCode "run-watch-side-by-side-smoke.ps1"
}

function Write-Summary {
    New-Item -ItemType Directory -Force -Path $ResolvedOutputDir | Out-Null
    $timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $summaryPath = Join-Path $ResolvedOutputDir "watch-full-validation-$timestamp.txt"

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("Galaxy Watch alarm full validation")
    $lines.Add("GeneratedAt=$(Get-Date -Format o)")
    $lines.Add("PhoneSerial=$PhoneSerial")
    $lines.Add("WatchSerial=$WatchSerial")
    $lines.Add("OutputDir=$ResolvedOutputDir")
    $lines.Add("")

    foreach ($result in $StepResults) {
        $duration = [int](New-TimeSpan -Start $result.StartedAt -End $result.EndedAt).TotalSeconds
        $line = "[$($result.Status)] $($result.Step) (${duration}s)"
        if (-not [string]::IsNullOrWhiteSpace($result.Error)) {
            $line += " - $($result.Error)"
        }
        $lines.Add($line)
    }

    $lines | Set-Content -Encoding UTF8 -LiteralPath $summaryPath
    Write-Host ""
    Write-Host "Validation summary: $summaryPath"
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "adb not found: $AdbPath"
}

Push-Location $RootDir
try {
    try {
        Invoke-ValidationStep "Source preflight" {
            & powershell -ExecutionPolicy Bypass -File .\scripts\validate-watch-integration-source.ps1
            Assert-LastExitCode "validate-watch-integration-source.ps1"
            & powershell -ExecutionPolicy Bypass -File .\scripts\test-watch-smoke-assertions.ps1
            Assert-LastExitCode "test-watch-smoke-assertions.ps1"
        }

        Invoke-ValidationStep "Resolve connected devices" {
            Resolve-DeviceSerials
        }

        if (-not $SkipInstall) {
            Invoke-ValidationStep "Build/install side-by-side APKs" {
                $installArgs = @(
                    "-ExecutionPolicy", "Bypass",
                    "-File", ".\scripts\install-watch-side-by-side.ps1",
                    "-PhoneSerial", $PhoneSerial,
                    "-WatchSerial", $WatchSerial,
                    "-AdbPath", $AdbPath,
                    "-JavaHome", $JavaHome
                )
                if ($SkipBuild) {
                    $installArgs += "-SkipBuild"
                }
                if ($SkipLaunch) {
                    $installArgs += "-SkipLaunch"
                }
                & powershell @installArgs
                Assert-LastExitCode "install-watch-side-by-side.ps1"
            }
        } else {
            if (-not $SkipBuild) {
                Invoke-ValidationStep "Build side-by-side APKs" {
                    Invoke-Gradle :app:assembleSideBySide :wear:assembleSideBySide
                }
            }
            Invoke-ValidationStep "Verify installed packages" {
                & powershell -ExecutionPolicy Bypass -File .\scripts\verify-watch-side-by-side.ps1 `
                    -PhoneSerial $PhoneSerial `
                    -WatchSerial $WatchSerial `
                    -AdbPath $AdbPath
                Assert-LastExitCode "verify-watch-side-by-side.ps1"
            }
        }

        Invoke-ValidationStep "Preview delivery smoke" {
            Invoke-Smoke -Scenario preview
        }

        if (-not $SkipFallbackSmoke) {
            Invoke-ValidationStep "Notification-blocked fallback smoke" {
                Invoke-Smoke -Scenario preview -BlockWatchNotifications -ExpectedDisplayMode fallback
            }
        }

        if (-not $SkipOrphanSmoke) {
            Invoke-ValidationStep "Orphaned watch alarm stale-control smoke" {
                Invoke-Smoke -Scenario orphan
            }
        }

        Invoke-ValidationStep "Watch stop control smoke" {
            Invoke-Smoke -Scenario stop
        }

        Invoke-ValidationStep "Watch snooze control smoke" {
            Invoke-Smoke -Scenario snooze
        }

        if (-not $SkipHardwareKeySmoke) {
            Invoke-ValidationStep "Watch hardware-key stop control smoke" {
                Invoke-Smoke -Scenario stop -AutoWatchActionSource hardwareKey
            }

            Invoke-ValidationStep "Watch hardware-key snooze control smoke" {
                Invoke-Smoke -Scenario snooze -AutoWatchActionSource hardwareKey
            }
        }

        Write-Host ""
        Write-Host "Full watch alarm validation passed."
    } finally {
        Write-Summary
    }
} catch {
    Write-Host ""
    Write-Host "Full watch alarm validation failed at step: $FailedStep"
    throw
} finally {
    Pop-Location
}
