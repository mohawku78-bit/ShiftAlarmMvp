param(
    [Parameter(Mandatory = $true)]
    [string]$PhoneSerial,
    [Parameter(Mandatory = $true)]
    [string]$WatchSerial,
    [string]$AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$JavaHome = "C:\Program Files\Android\Android Studio1\jbr",
    [string]$OutputDir = "manual-validation\watch-alarm",
    [int]$PreviewWaitSeconds = 20,
    [int]$ControlWaitSeconds = 20,
    [int]$AutoWatchActionDelaySeconds = 4,
    [int]$AutoWatchActionAttempts = 3,
    [int]$AutoWatchActionRetrySeconds = 2,
    [switch]$SkipBuild,
    [switch]$SkipInstall,
    [switch]$SkipLaunch
)

$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$ResolvedOutputDir = Join-Path $RootDir $OutputDir
$StepResults = New-Object System.Collections.Generic.List[object]
$FailedStep = $null

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

function Invoke-Smoke {
    param(
        [ValidateSet("preview", "stop", "snooze")]
        [string]$Scenario
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
        $args += @(
            "-Mode", "control",
            "-Label", "Full validation $Scenario",
            "-AutoWatchAction", $Scenario,
            "-ExpectedAction", $Scenario,
            "-AutoWatchActionDelaySeconds", $AutoWatchActionDelaySeconds.ToString(),
            "-AutoWatchActionAttempts", $AutoWatchActionAttempts.ToString(),
            "-AutoWatchActionRetrySeconds", $AutoWatchActionRetrySeconds.ToString(),
            "-WaitSeconds", $ControlWaitSeconds.ToString()
        )
    }

    & powershell @args
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
            }
        }

        Invoke-ValidationStep "Preview delivery smoke" {
            Invoke-Smoke -Scenario preview
        }

        Invoke-ValidationStep "Watch stop control smoke" {
            Invoke-Smoke -Scenario stop
        }

        Invoke-ValidationStep "Watch snooze control smoke" {
            Invoke-Smoke -Scenario snooze
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
