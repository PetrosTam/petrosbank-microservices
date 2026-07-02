$ErrorActionPreference = "Stop"

$BaseUrl = "http://localhost:8080"
$CorrelationId = "petros-api-smoke-test-" + [guid]::NewGuid().ToString()

Write-Host ""
Write-Host "Banking App - API Smoke Test" -ForegroundColor Cyan
Write-Host "============================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Base URL: $BaseUrl"
Write-Host "Correlation ID: $CorrelationId"
Write-Host ""

$headers = @{
    "X-Correlation-ID" = $CorrelationId
}

try {
    Write-Host "[1/7] Testing public gateway endpoint..." -ForegroundColor Yellow

    $publicResponse = Invoke-RestMethod `
        -Uri "$BaseUrl/api/v1/users/accessAll" `
        -Method Get `
        -Headers $headers

    Write-Host "[OK] Public endpoint response: $publicResponse" -ForegroundColor Green

    Write-Host ""
    Write-Host "[2/7] Registering test user..." -ForegroundColor Yellow

    $uniqueId = [guid]::NewGuid().ToString("N").Substring(0, 8)
    $email = "smoke-test-$uniqueId@test.com"
    $password = "Password123!"

    $registerBody = @{
        firstName = "Smoke"
        lastName = "Test"
        email = $email
        password = $password
        phone = "99999999"
        roles = @("ROLE_USER")
    } | ConvertTo-Json

    $registerResponse = Invoke-RestMethod `
        -Uri "$BaseUrl/api/v1/users/register" `
        -Method Post `
        -ContentType "application/json" `
        -Headers $headers `
        -Body $registerBody

    Write-Host "[OK] Registered user: $email" -ForegroundColor Green

    Write-Host ""
    Write-Host "[3/7] Logging in..." -ForegroundColor Yellow

    $loginBody = @{
        email = $email
        password = $password
    } | ConvertTo-Json

    $loginResponse = Invoke-RestMethod `
        -Uri "$BaseUrl/api/v1/users/login" `
        -Method Post `
        -ContentType "application/json" `
        -Headers $headers `
        -Body $loginBody

    if (-not $loginResponse.accessToken) {
        throw "Login response did not contain an access token."
    }

    if (-not $loginResponse.refreshToken) {
        throw "Login response did not contain a refresh token."
    }

    $accessToken = $loginResponse.accessToken
    $refreshToken = $loginResponse.refreshToken

    Write-Host "[OK] Login successful. Access token and refresh token received." -ForegroundColor Green

    Write-Host ""
    Write-Host "[4/7] Refreshing access token..." -ForegroundColor Yellow

    $refreshBody = @{
        refreshToken = $refreshToken
    } | ConvertTo-Json

    $refreshResponse = Invoke-RestMethod `
        -Uri "$BaseUrl/api/v1/users/refresh-token" `
        -Method Post `
        -ContentType "application/json" `
        -Headers $headers `
        -Body $refreshBody

    if (-not $refreshResponse.accessToken) {
        throw "Refresh response did not contain a new access token."
    }

    Write-Host "[OK] Refresh token flow works." -ForegroundColor Green

    Write-Host ""
    Write-Host "[5/7] Creating account through API Gateway..." -ForegroundColor Yellow

    $authHeaders = @{
        Authorization = "Bearer $accessToken"
        "X-Correlation-ID" = $CorrelationId
    }

    $accountBody = @{
        accountType = "SAVINGS"
        accountName = "Smoke Test Savings Account"
    } | ConvertTo-Json

    $accountResponse = Invoke-RestMethod `
        -Uri "$BaseUrl/api/v1/accounts" `
        -Method Post `
        -ContentType "application/json" `
        -Headers $authHeaders `
        -Body $accountBody

    Write-Host "[OK] Account created successfully." -ForegroundColor Green

    if ($accountResponse.accountNumber) {
        Write-Host "Account Number: $($accountResponse.accountNumber)" -ForegroundColor Green
    }

    Write-Host ""
    Write-Host "[6/7] Logging out and revoking refresh token..." -ForegroundColor Yellow

    $logoutBody = @{
        refreshToken = $refreshToken
    } | ConvertTo-Json

    $logoutResponse = Invoke-WebRequest `
        -UseBasicParsing `
        -Uri "$BaseUrl/api/v1/users/logout" `
        -Method Post `
        -ContentType "application/json" `
        -Headers $headers `
        -Body $logoutBody

    if ($logoutResponse.StatusCode -ne 204) {
        throw "Logout did not return HTTP 204."
    }

    Write-Host "[OK] Logout successful. Refresh token revoked." -ForegroundColor Green

    Write-Host ""
    Write-Host "[7/7] Verifying revoked refresh token is rejected..." -ForegroundColor Yellow

    $revokedTokenRejected = $false

    try {
        Invoke-RestMethod `
            -Uri "$BaseUrl/api/v1/users/refresh-token" `
            -Method Post `
            -ContentType "application/json" `
            -Headers $headers `
            -Body $refreshBody | Out-Null
    }
    catch {
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode

            if ($statusCode -eq 401) {
                $revokedTokenRejected = $true
            } else {
                throw "Expected HTTP 401 for revoked refresh token, but received HTTP $statusCode."
            }
        } else {
            throw
        }
    }

    if (-not $revokedTokenRejected) {
        throw "Revoked refresh token was accepted unexpectedly."
    }

    Write-Host "[OK] Revoked refresh token correctly rejected with HTTP 401." -ForegroundColor Green

    Write-Host ""
    Write-Host "API smoke test completed successfully." -ForegroundColor Green
    exit 0
}
catch {
    Write-Host ""
    Write-Host "API smoke test failed." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}