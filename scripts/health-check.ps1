[CmdletBinding()]
param(
    [ValidateRange(1, 10)]
    [int]$MaxAttempts = 6,

    [ValidateRange(1, 60)]
    [int]$InitialDelaySeconds = 5,

    [ValidateRange(1, 60)]
    [int]$MaxDelaySeconds = 15,

    [ValidateRange(1, 60)]
    [int]$RequestTimeoutSeconds = 5
)

$ErrorActionPreference = "Stop"

# ANSI formatting supported by the IntelliJ terminal.
$escape = [char]27
$bold = "${escape}[1m"
$brightRed = "${escape}[91m"
$reset = "${escape}[0m"

Write-Host ""

$title = "PetrosBank Microservices - Local Health Check"

Write-Host "${bold}${title}${reset}" -ForegroundColor Cyan
Write-Host ("=" * $title.Length) -ForegroundColor Cyan
Write-Host ""

$services = @(
    [PSCustomObject]@{
        Name      = "API Gateway"
        Url       = "http://localhost:8080/actuator/health"
        IsHealthy = $false
    },
    [PSCustomObject]@{
        Name      = "User Service"
        Url       = "http://localhost:8081/actuator/health"
        IsHealthy = $false
    },
    [PSCustomObject]@{
        Name      = "Account Service"
        Url       = "http://localhost:8082/actuator/health"
        IsHealthy = $false
    },
    [PSCustomObject]@{
        Name      = "Transaction Service"
        Url       = "http://localhost:8083/actuator/health"
        IsHealthy = $false
    },
    [PSCustomObject]@{
        Name      = "Notification Service"
        Url       = "http://localhost:8084/actuator/health"
        IsHealthy = $false
    },
    [PSCustomObject]@{
        Name      = "Eureka Server"
        Url       = "http://localhost:8761/actuator/health"
        IsHealthy = $false
    }
)

function Get-RetryDelaySeconds {
    param(
        [Parameter(Mandatory = $true)]
        [int]$RetryNumber
    )

    $calculatedDelay = [int](
        $InitialDelaySeconds *
        [math]::Pow(2, $RetryNumber - 1)
    )

    return [int](
        [math]::Min(
            $calculatedDelay,
            $MaxDelaySeconds
        )
    )
}

function Get-RetryPolicyMessage {
    if ($MaxAttempts -le 1) {
        return "Retry policy: 1 total attempt per service (no retries)."
    }

    $retryCount = $MaxAttempts - 1

    $retryWord = if ($retryCount -eq 1) {
        "retry"
    }
    else {
        "retries"
    }

    $retryDelays = @()

    for (
        $retryNumber = 1;
        $retryNumber -le $retryCount;
        $retryNumber++
    ) {
        $delaySeconds = Get-RetryDelaySeconds `
            -RetryNumber $retryNumber

        $retryDelays += "${delaySeconds}s"
    }

    $retryDelayText = $retryDelays -join ", "

    return (
        "Retry policy: $MaxAttempts total attempts per service " +
        "(1 initial attempt + $retryCount $retryWord; " +
        "delays: $retryDelayText)."
    )
}

function Wait-WithSpinner {
    param(
        [Parameter(Mandatory = $true)]
        [int]$HealthyCount,

        [Parameter(Mandatory = $true)]
        [int]$TotalCount,

        [Parameter(Mandatory = $true)]
        [int]$UnhealthyCount,

        [Parameter(Mandatory = $true)]
        [int]$DelaySeconds
    )

    $spinnerFrames = @("|", "/", "-", "\")
    $frameIntervalMilliseconds = 125

    $totalIterations = [math]::Max(
        1,
        [int][math]::Ceiling(
            ($DelaySeconds * 1000) /
            $frameIntervalMilliseconds
        )
    )

    $serviceWord = if ($UnhealthyCount -eq 1) {
        "service"
    }
    else {
        "services"
    }

    for (
        $iteration = 0;
        $iteration -lt $totalIterations;
        $iteration++
    ) {
        $frame = $spinnerFrames[
            $iteration % $spinnerFrames.Count
        ]

        $elapsedMilliseconds =
            $iteration * $frameIntervalMilliseconds

        $remainingMilliseconds = [math]::Max(
            0,
            ($DelaySeconds * 1000) -
            $elapsedMilliseconds
        )

        $remainingSeconds = [math]::Max(
            1,
            [int][math]::Ceiling(
                $remainingMilliseconds / 1000
            )
        )

        $secondWord = if ($remainingSeconds -eq 1) {
            "second"
        }
        else {
            "seconds"
        }

        $message =
            "[$frame] $HealthyCount/$TotalCount services recovered; " +
            "retrying $UnhealthyCount unhealthy $serviceWord " +
            "in $remainingSeconds $secondWord..."

        Write-Host `
            ("`r" + $message.PadRight(140)) `
            -ForegroundColor Yellow `
            -NoNewline

        Start-Sleep `
            -Milliseconds $frameIntervalMilliseconds
    }

    $finalMessage =
        "[*] $HealthyCount/$TotalCount services recovered; " +
        "retrying $UnhealthyCount unhealthy $serviceWord now..."

    Write-Host `
        ("`r" + $finalMessage.PadRight(140)) `
        -ForegroundColor Yellow
}

$retryPolicyMessage = Get-RetryPolicyMessage

Write-Host $retryPolicyMessage -ForegroundColor DarkGray
Write-Host ""

for (
    $attempt = 1;
    $attempt -le $MaxAttempts;
    $attempt++
) {
    $pendingServices = @(
        $services |
            Where-Object { -not $_.IsHealthy }
    )

    if ($pendingServices.Count -eq 0) {
        break
    }

    $roundResults = @()
    $failedThisRound = @()

    foreach ($service in $pendingServices) {
        try {
            $response = Invoke-RestMethod `
                -Uri $service.Url `
                -Method Get `
                -TimeoutSec $RequestTimeoutSeconds

            if ($response.status -eq "UP") {
                $service.IsHealthy = $true

                $roundResults += [PSCustomObject]@{
                    Service = $service
                    Result  = "UP"
                }

                continue
            }
        }
        catch {
            # A connection or HTTP error means the service
            # remains unavailable for the current attempt.
        }

        $failedThisRound += $service

        $roundResults += [PSCustomObject]@{
            Service = $service
            Result  = "FAILED"
        }
    }

    if ($attempt -gt 1) {
        Write-Host ""
    }

    $attemptHeading = "Attempt $attempt/$MaxAttempts"

    Write-Host `
        "${bold}${attemptHeading}${reset}" `
        -ForegroundColor Cyan

    Write-Host `
        ("-" * $attemptHeading.Length) `
        -ForegroundColor DarkCyan

    Write-Host ""

    foreach ($roundResult in $roundResults) {
        $service = $roundResult.Service

        if ($roundResult.Result -eq "UP") {
            Write-Host `
                "[UP]     $($service.Name)" `
                -ForegroundColor Green

            continue
        }

        if ($attempt -lt $MaxAttempts) {
            Write-Host `
                "[FAILED] $($service.Name)" `
                -ForegroundColor Yellow
        }
        else {
            Write-Host `
                "${brightRed}[FAILED] $($service.Name)${reset}"
        }
    }

    if (
        $failedThisRound.Count -gt 0 -and
        $attempt -lt $MaxAttempts
    ) {
        $delaySeconds =
            Get-RetryDelaySeconds -RetryNumber $attempt

        $healthyCount = @(
            $services |
                Where-Object { $_.IsHealthy }
        ).Count

        $unhealthyCount = $failedThisRound.Count
        $totalCount = $services.Count

        Write-Host ""

        Wait-WithSpinner `
            -HealthyCount $healthyCount `
            -TotalCount $totalCount `
            -UnhealthyCount $unhealthyCount `
            -DelaySeconds $delaySeconds
    }
}

$failedServices = @(
    $services |
        Where-Object { -not $_.IsHealthy }
)

Write-Host ""

if ($failedServices.Count -eq 0) {
    Write-Host `
        "All services are healthy." `
        -ForegroundColor Green

    exit 0
}

$attemptPhrase = if ($MaxAttempts -eq 1) {
    "1 attempt"
}
else {
    "$MaxAttempts total attempts"
}

if ($failedServices.Count -eq $services.Count) {
    $finalMessage =
        "All services are unhealthy after $attemptPhrase each."
}
elseif ($failedServices.Count -eq 1) {
    $finalMessage =
        "The following service is unhealthy after ${attemptPhrase}: " +
        "$($failedServices[0].Name)."
}
else {
    $failedServiceNames =
        ($failedServices.Name) -join ", "

    $finalMessage =
        "The following services are unhealthy after " +
        "$attemptPhrase each: $failedServiceNames."
}

Write-Host "${brightRed}${finalMessage}${reset}"

exit 1