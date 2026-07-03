[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",

    [ValidateRange(1, 10)]
    [int]$ReadinessMaxAttempts = 6,

    [ValidateRange(1, 60)]
    [int]$ReadinessInitialDelaySeconds = 5,

    [ValidateRange(1, 60)]
    [int]$ReadinessMaxDelaySeconds = 15,

    [ValidateRange(1, 10)]
    [int]$RouteReadinessMaxAttempts = 5,

    [ValidateRange(1, 60)]
    [int]$RouteReadinessInitialDelaySeconds = 3,

    [ValidateRange(1, 60)]
    [int]$RouteReadinessMaxDelaySeconds = 10,

    [ValidateRange(1, 5)]
    [int]$TransientRequestMaxAttempts = 3,

    [ValidateRange(1, 60)]
    [int]$RequestTimeoutSeconds = 10
)

$ErrorActionPreference = "Stop"

# ANSI formatting supported by the IntelliJ terminal.
$escape = [char]27
$bold = "${escape}[1m"
$brightRed = "${escape}[91m"
$reset = "${escape}[0m"

$CorrelationId =
    "petros-api-smoke-test-" +
    [guid]::NewGuid().ToString()

$title = "PetrosBank Microservices - API Smoke Test"

function Get-BackoffDelaySeconds {
    param(
        [Parameter(Mandatory = $true)]
        [int]$RetryNumber,

        [Parameter(Mandatory = $true)]
        [int]$InitialDelaySeconds,

        [Parameter(Mandatory = $true)]
        [int]$MaximumDelaySeconds
    )

    $calculatedDelay = [int](
        $InitialDelaySeconds *
        [math]::Pow(2, $RetryNumber - 1)
    )

    return [int](
        [math]::Min(
            $calculatedDelay,
            $MaximumDelaySeconds
        )
    )
}

function Get-RetryPolicyMessage {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PolicyName,

        [Parameter(Mandatory = $true)]
        [int]$MaxAttempts,

        [Parameter(Mandatory = $true)]
        [int]$InitialDelaySeconds,

        [Parameter(Mandatory = $true)]
        [int]$MaximumDelaySeconds
    )

    if ($MaxAttempts -le 1) {
        return "${PolicyName}: 1 total attempt (no retries)."
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
        $delaySeconds = Get-BackoffDelaySeconds `
            -RetryNumber $retryNumber `
            -InitialDelaySeconds $InitialDelaySeconds `
            -MaximumDelaySeconds $MaximumDelaySeconds

        $retryDelays += "${delaySeconds}s"
    }

    $retryDelayText = $retryDelays -join ", "

    return (
        "${PolicyName}: $MaxAttempts total attempts " +
        "(1 initial attempt + $retryCount $retryWord; " +
        "delays: $retryDelayText)."
    )
}

function Get-HttpStatusCode {
    param(
        [Parameter(Mandatory = $true)]
        [System.Management.Automation.ErrorRecord]$ErrorRecord
    )

    $response = $ErrorRecord.Exception.Response

    if ($null -eq $response) {
        return $null
    }

    try {
        return [int]$response.StatusCode
    }
    catch {
        return $null
    }
}

function Test-IsTransientFailure {
    param(
        $StatusCode
    )

    # No HTTP response usually means a connection or transport failure.
    if ($null -eq $StatusCode) {
        return $true
    }

    return $StatusCode -in @(502, 503, 504)
}

function Get-TransientFailureDescription {
    param(
        $StatusCode
    )

    if ($null -eq $StatusCode) {
        return "Connection failure"
    }

    switch ($StatusCode) {
        502 {
            return "HTTP 502 Bad Gateway"
        }
        503 {
            return "HTTP 503 Service Unavailable"
        }
        504 {
            return "HTTP 504 Gateway Timeout"
        }
        default {
            return "HTTP $StatusCode"
        }
    }
}

function Wait-WithSpinner {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message,

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

    for (
        $iteration = 0;
        $iteration -lt $totalIterations;
        $iteration++
    ) {
        $frame =
            $spinnerFrames[
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

        $currentMessage =
            "[$frame] $Message in " +
            "$remainingSeconds $secondWord..."

        Write-Host `
            ("`r" + $currentMessage.PadRight(150)) `
            -ForegroundColor Yellow `
            -NoNewline

        Start-Sleep `
            -Milliseconds $frameIntervalMilliseconds
    }

    $finalMessage = "[*] $Message now..."

    Write-Host `
        ("`r" + $finalMessage.PadRight(150)) `
        -ForegroundColor Yellow
}

function Write-SectionHeading {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Heading
    )

    Write-Host `
        "${bold}${Heading}${reset}" `
        -ForegroundColor Cyan

    Write-Host `
        ("-" * $Heading.Length) `
        -ForegroundColor DarkCyan
}

function Write-AttemptHeading {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Attempt,

        [Parameter(Mandatory = $true)]
        [int]$MaximumAttempts
    )

    $heading = "Attempt $Attempt/$MaximumAttempts"

    Write-Host `
        "${bold}${heading}${reset}" `
        -ForegroundColor Cyan

    Write-Host `
        ("-" * $heading.Length) `
        -ForegroundColor DarkCyan

    Write-Host ""
}

function Write-Step {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Number,

        [Parameter(Mandatory = $true)]
        [int]$Total,

        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Host ""

    $heading = "[$Number/$Total] $Message"

    Write-Host `
        "${bold}${heading}${reset}" `
        -ForegroundColor Yellow
}

function Invoke-WithTransientRetry {
    param(
        [Parameter(Mandatory = $true)]
        [string]$OperationName,

        [Parameter(Mandatory = $true)]
        [scriptblock]$Operation,

        [switch]$RetrySafe
    )

    $maximumAttempts = if ($RetrySafe) {
        $TransientRequestMaxAttempts
    }
    else {
        1
    }

    for (
        $attempt = 1;
        $attempt -le $maximumAttempts;
        $attempt++
    ) {
        try {
            return (& $Operation)
        }
        catch {
            $statusCode =
                Get-HttpStatusCode -ErrorRecord $_

            $isTransient =
                Test-IsTransientFailure `
                    -StatusCode $statusCode

            $canRetry =
                $RetrySafe -and
                $isTransient -and
                $attempt -lt $maximumAttempts

            if (-not $canRetry) {
                throw
            }

            $failureDescription =
                Get-TransientFailureDescription `
                    -StatusCode $statusCode

            Write-Host `
                "[FAILED] $OperationName - $failureDescription" `
                -ForegroundColor Yellow

            $delaySeconds =
                Get-BackoffDelaySeconds `
                    -RetryNumber $attempt `
                    -InitialDelaySeconds 2 `
                    -MaximumDelaySeconds 4

            Wait-WithSpinner `
                -Message (
                    "Retrying $OperationName " +
                    "(attempt $($attempt + 1)/$maximumAttempts)"
                ) `
                -DelaySeconds $delaySeconds
        }
    }
}

function Wait-ForRequiredDependencies {
    $dependencies = @(
        [PSCustomObject]@{
            Name    = "API Gateway"
            Url     = "$BaseUrl/actuator/health"
            IsReady = $false
        },
        [PSCustomObject]@{
            Name    = "User Service"
            Url     = "http://localhost:8081/actuator/health"
            IsReady = $false
        },
        [PSCustomObject]@{
            Name    = "Account Service"
            Url     = "http://localhost:8082/actuator/health"
            IsReady = $false
        },
        [PSCustomObject]@{
            Name    = "Eureka Server"
            Url     = "http://localhost:8761/actuator/health"
            IsReady = $false
        }
    )

    Write-SectionHeading `
        -Heading "Component readiness check"

    Write-Host ""

    for (
        $attempt = 1;
        $attempt -le $ReadinessMaxAttempts;
        $attempt++
    ) {
        $pendingDependencies = @(
            $dependencies |
                Where-Object { -not $_.IsReady }
        )

        if ($pendingDependencies.Count -eq 0) {
            break
        }

        if ($attempt -gt 1) {
            Write-Host ""
        }

        Write-AttemptHeading `
            -Attempt $attempt `
            -MaximumAttempts $ReadinessMaxAttempts

        $failedThisRound = @()

        foreach ($dependency in $pendingDependencies) {
            try {
                $response = Invoke-RestMethod `
                    -Uri $dependency.Url `
                    -Method Get `
                    -TimeoutSec $RequestTimeoutSeconds

                if ($response.status -eq "UP") {
                    $dependency.IsReady = $true

                    Write-Host `
                        "[UP]     $($dependency.Name)" `
                        -ForegroundColor Green

                    continue
                }
            }
            catch {
                # The dependency remains unavailable for this attempt.
            }

            $failedThisRound += $dependency

            if ($attempt -lt $ReadinessMaxAttempts) {
                Write-Host `
                    "[FAILED] $($dependency.Name)" `
                    -ForegroundColor Yellow
            }
            else {
                Write-Host `
                    "${brightRed}[FAILED] $($dependency.Name)${reset}"
            }
        }

        if (
            $failedThisRound.Count -gt 0 -and
            $attempt -lt $ReadinessMaxAttempts
        ) {
            $delaySeconds =
                Get-BackoffDelaySeconds `
                    -RetryNumber $attempt `
                    -InitialDelaySeconds $ReadinessInitialDelaySeconds `
                    -MaximumDelaySeconds $ReadinessMaxDelaySeconds

            $readyCount = @(
                $dependencies |
                    Where-Object { $_.IsReady }
            ).Count

            $unavailableCount =
                $failedThisRound.Count

            $dependencyWord =
                if ($unavailableCount -eq 1) {
                    "dependency"
                }
                else {
                    "dependencies"
                }

            Write-Host ""

            Wait-WithSpinner `
                -Message (
                    "$readyCount/$($dependencies.Count) dependencies ready; " +
                    "retrying $unavailableCount unavailable " +
                    $dependencyWord
                ) `
                -DelaySeconds $delaySeconds
        }
    }

    $unavailableDependencies = @(
        $dependencies |
            Where-Object { -not $_.IsReady }
    )

    if ($unavailableDependencies.Count -gt 0) {
        $dependencyNames =
            ($unavailableDependencies.Name) -join ", "

        throw (
            "Required dependencies are unavailable after " +
            "$ReadinessMaxAttempts attempts: " +
            "$dependencyNames."
        )
    }

    Write-Host ""
    Write-Host `
        "All required components are ready." `
        -ForegroundColor Green
}

function Wait-ForGatewayRoute {
    param(
        [Parameter(Mandatory = $true)]
        [hashtable]$Headers
    )

    $routeName = "API Gateway -> User Service route"
    $routeUrl = "$BaseUrl/api/v1/users/accessAll"

    Write-Host ""
    Write-SectionHeading `
        -Heading "Gateway route readiness check"

    Write-Host ""

    for (
        $attempt = 1;
        $attempt -le $RouteReadinessMaxAttempts;
        $attempt++
    ) {
        if ($attempt -gt 1) {
            Write-Host ""
        }

        Write-AttemptHeading `
            -Attempt $attempt `
            -MaximumAttempts $RouteReadinessMaxAttempts

        $routeIsReady = $false

        try {
            $routeResponse = Invoke-RestMethod `
                -Uri $routeUrl `
                -Method Get `
                -Headers $Headers `
                -TimeoutSec $RequestTimeoutSeconds

            if ($null -ne $routeResponse) {
                $routeIsReady = $true
            }
        }
        catch {
            # HTTP 502, 503, 504 or a connection error means
            # that Gateway routing is not ready yet.
        }

        if ($routeIsReady) {
            Write-Host `
                "[UP]     $routeName" `
                -ForegroundColor Green

            Write-Host ""
            Write-Host `
                "Gateway routing is ready." `
                -ForegroundColor Green

            return
        }

        if ($attempt -lt $RouteReadinessMaxAttempts) {
            Write-Host `
                "[FAILED] $routeName" `
                -ForegroundColor Yellow

            $delaySeconds =
                Get-BackoffDelaySeconds `
                    -RetryNumber $attempt `
                    -InitialDelaySeconds $RouteReadinessInitialDelaySeconds `
                    -MaximumDelaySeconds $RouteReadinessMaxDelaySeconds

            Write-Host ""

            Wait-WithSpinner `
                -Message "Waiting for Gateway service discovery" `
                -DelaySeconds $delaySeconds
        }
        else {
            Write-Host `
                "${brightRed}[FAILED] $routeName${reset}"
        }
    }

    throw (
        "Gateway routing remained unavailable after " +
        "$RouteReadinessMaxAttempts attempts."
    )
}

Write-Host ""
Write-Host `
    "${bold}${title}${reset}" `
    -ForegroundColor Cyan

Write-Host `
    ("=" * $title.Length) `
    -ForegroundColor Cyan

Write-Host ""

$readinessPolicyMessage =
    Get-RetryPolicyMessage `
        -PolicyName "Component readiness policy" `
        -MaxAttempts $ReadinessMaxAttempts `
        -InitialDelaySeconds $ReadinessInitialDelaySeconds `
        -MaximumDelaySeconds $ReadinessMaxDelaySeconds

$routeReadinessPolicyMessage =
    Get-RetryPolicyMessage `
        -PolicyName "Gateway route readiness policy" `
        -MaxAttempts $RouteReadinessMaxAttempts `
        -InitialDelaySeconds $RouteReadinessInitialDelaySeconds `
        -MaximumDelaySeconds $RouteReadinessMaxDelaySeconds

$transientPolicyMessage =
    Get-RetryPolicyMessage `
        -PolicyName "Transient request policy" `
        -MaxAttempts $TransientRequestMaxAttempts `
        -InitialDelaySeconds 2 `
        -MaximumDelaySeconds 4

Write-Host `
    $readinessPolicyMessage `
    -ForegroundColor DarkGray

Write-Host `
    $routeReadinessPolicyMessage `
    -ForegroundColor DarkGray

Write-Host `
    "$transientPolicyMessage Retry-safe requests only." `
    -ForegroundColor DarkGray

Write-Host ""

Write-Host "Base URL: $BaseUrl"
Write-Host "Correlation ID: $CorrelationId"
Write-Host ""

$headers = @{
    "X-Correlation-ID" = $CorrelationId
}

try {
    Wait-ForRequiredDependencies

    Wait-ForGatewayRoute `
        -Headers $headers

    Write-Step `
        -Number 1 `
        -Total 7 `
        -Message "Testing public gateway endpoint..."

    $publicResponse =
        Invoke-WithTransientRetry `
            -OperationName "public gateway request" `
            -RetrySafe `
            -Operation {
                Invoke-RestMethod `
                    -Uri "$BaseUrl/api/v1/users/accessAll" `
                    -Method Get `
                    -Headers $headers `
                    -TimeoutSec $RequestTimeoutSeconds
            }

    Write-Host `
        "[OK] Public endpoint response: $publicResponse" `
        -ForegroundColor Green

    Write-Step `
        -Number 2 `
        -Total 7 `
        -Message "Registering test user..."

    $uniqueId =
        [guid]::NewGuid().
            ToString("N").
            Substring(0, 8)

    $email = "smoke-test-$uniqueId@test.com"
    $password = "Password123!"

    $registerBody = @{
        firstName = "Smoke"
        lastName  = "Test"
        email     = $email
        password  = $password
        phone     = "99999999"
        roles     = @("ROLE_USER")
    } | ConvertTo-Json

    # Registration is intentionally not retried because it creates data.
    $registerResponse =
        Invoke-WithTransientRetry `
            -OperationName "user registration request" `
            -Operation {
                Invoke-RestMethod `
                    -Uri "$BaseUrl/api/v1/users/register" `
                    -Method Post `
                    -ContentType "application/json" `
                    -Headers $headers `
                    -Body $registerBody `
                    -TimeoutSec $RequestTimeoutSeconds
            }

    Write-Host `
        "[OK] Registered user: $email" `
        -ForegroundColor Green

    Write-Step `
        -Number 3 `
        -Total 7 `
        -Message "Logging in..."

    $loginBody = @{
        email    = $email
        password = $password
    } | ConvertTo-Json

    $loginResponse =
        Invoke-WithTransientRetry `
            -OperationName "login request" `
            -RetrySafe `
            -Operation {
                Invoke-RestMethod `
                    -Uri "$BaseUrl/api/v1/users/login" `
                    -Method Post `
                    -ContentType "application/json" `
                    -Headers $headers `
                    -Body $loginBody `
                    -TimeoutSec $RequestTimeoutSeconds
            }

    if (-not $loginResponse.accessToken) {
        throw (
            "Login response did not contain " +
            "an access token."
        )
    }

    if (-not $loginResponse.refreshToken) {
        throw (
            "Login response did not contain " +
            "a refresh token."
        )
    }

    $accessToken =
        $loginResponse.accessToken

    $refreshToken =
        $loginResponse.refreshToken

    Write-Host `
        "[OK] Login successful. Access token and refresh token received." `
        -ForegroundColor Green

    Write-Step `
        -Number 4 `
        -Total 7 `
        -Message "Refreshing access token..."

    $refreshBody = @{
        refreshToken = $refreshToken
    } | ConvertTo-Json

    $refreshResponse =
        Invoke-WithTransientRetry `
            -OperationName "refresh-token request" `
            -RetrySafe `
            -Operation {
                Invoke-RestMethod `
                    -Uri "$BaseUrl/api/v1/users/refresh-token" `
                    -Method Post `
                    -ContentType "application/json" `
                    -Headers $headers `
                    -Body $refreshBody `
                    -TimeoutSec $RequestTimeoutSeconds
            }

    if (-not $refreshResponse.accessToken) {
        throw (
            "Refresh response did not contain " +
            "a new access token."
        )
    }

    Write-Host `
        "[OK] Refresh token flow works." `
        -ForegroundColor Green

    Write-Step `
        -Number 5 `
        -Total 7 `
        -Message "Creating account through API Gateway..."

    $authHeaders = @{
        Authorization      = "Bearer $accessToken"
        "X-Correlation-ID" = $CorrelationId
    }

    $accountBody = @{
        accountType = "SAVINGS"
        accountName = "Smoke Test Savings Account"
    } | ConvertTo-Json

    # Account creation is intentionally not retried because it creates data.
    $accountResponse =
        Invoke-WithTransientRetry `
            -OperationName "account creation request" `
            -Operation {
                Invoke-RestMethod `
                    -Uri "$BaseUrl/api/v1/accounts" `
                    -Method Post `
                    -ContentType "application/json" `
                    -Headers $authHeaders `
                    -Body $accountBody `
                    -TimeoutSec $RequestTimeoutSeconds
            }

    Write-Host `
        "[OK] Account created successfully." `
        -ForegroundColor Green

    if ($accountResponse.accountNumber) {
        Write-Host `
            "Account Number: $($accountResponse.accountNumber)" `
            -ForegroundColor Green
    }

    Write-Step `
        -Number 6 `
        -Total 7 `
        -Message "Logging out and revoking refresh token..."

    $logoutBody = @{
        refreshToken = $refreshToken
    } | ConvertTo-Json

    # Logout is intentionally not retried because the first request
    # may already have revoked the token even if its response was lost.
    $logoutResponse =
        Invoke-WithTransientRetry `
            -OperationName "logout request" `
            -Operation {
                Invoke-WebRequest `
                    -UseBasicParsing `
                    -Uri "$BaseUrl/api/v1/users/logout" `
                    -Method Post `
                    -ContentType "application/json" `
                    -Headers $headers `
                    -Body $logoutBody `
                    -TimeoutSec $RequestTimeoutSeconds
            }

    if ($logoutResponse.StatusCode -ne 204) {
        throw "Logout did not return HTTP 204."
    }

    Write-Host `
        "[OK] Logout successful. Refresh token revoked." `
        -ForegroundColor Green

    Write-Step `
        -Number 7 `
        -Total 7 `
        -Message "Verifying revoked refresh token is rejected..."

    $revokedTokenRejected = $false

    for (
        $attempt = 1;
        $attempt -le $TransientRequestMaxAttempts;
        $attempt++
    ) {
        $requestSucceeded = $false

        try {
            Invoke-RestMethod `
                -Uri "$BaseUrl/api/v1/users/refresh-token" `
                -Method Post `
                -ContentType "application/json" `
                -Headers $headers `
                -Body $refreshBody `
                -TimeoutSec $RequestTimeoutSeconds |
                Out-Null

            $requestSucceeded = $true
        }
        catch {
            $statusCode =
                Get-HttpStatusCode -ErrorRecord $_

            if ($statusCode -eq 401) {
                $revokedTokenRejected = $true
                break
            }

            $isTransient =
                Test-IsTransientFailure `
                    -StatusCode $statusCode

            if (
                $isTransient -and
                $attempt -lt $TransientRequestMaxAttempts
            ) {
                $failureDescription =
                    Get-TransientFailureDescription `
                        -StatusCode $statusCode

                Write-Host `
                    "[FAILED] Revoked-token verification - $failureDescription" `
                    -ForegroundColor Yellow

                $delaySeconds =
                    Get-BackoffDelaySeconds `
                        -RetryNumber $attempt `
                        -InitialDelaySeconds 2 `
                        -MaximumDelaySeconds 4

                Wait-WithSpinner `
                    -Message (
                        "Retrying revoked-token verification " +
                        "(attempt $($attempt + 1)/" +
                        "$TransientRequestMaxAttempts)"
                    ) `
                    -DelaySeconds $delaySeconds

                continue
            }

            if ($null -ne $statusCode) {
                throw (
                    "Expected HTTP 401 for revoked refresh token, " +
                    "but received HTTP $statusCode."
                )
            }

            throw
        }

        if ($requestSucceeded) {
            throw (
                "Revoked refresh token was " +
                "accepted unexpectedly."
            )
        }
    }

    if (-not $revokedTokenRejected) {
        throw (
            "Revoked refresh token was " +
            "accepted unexpectedly."
        )
    }

    Write-Host `
        "[OK] Revoked refresh token correctly rejected with HTTP 401." `
        -ForegroundColor Green

    Write-Host ""
    Write-Host `
        "${bold}API smoke test completed successfully.${reset}" `
        -ForegroundColor Green

    exit 0
}
catch {
    Write-Host ""

    Write-Host `
        "${brightRed}${bold}API smoke test failed.${reset}"

    Write-Host `
        "${brightRed}$($_.Exception.Message)${reset}"

    exit 1
}