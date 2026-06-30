$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "Banking App - Local Health Check" -ForegroundColor Cyan
Write-Host "================================" -ForegroundColor Cyan
Write-Host ""

$services = @(
    @{ Name = "API Gateway"; Url = "http://localhost:8080/actuator/health" },
    @{ Name = "User Service"; Url = "http://localhost:8081/actuator/health" },
    @{ Name = "Account Service"; Url = "http://localhost:8082/actuator/health" },
    @{ Name = "Transaction Service"; Url = "http://localhost:8083/actuator/health" },
    @{ Name = "Notification Service"; Url = "http://localhost:8084/actuator/health" },
    @{ Name = "Eureka Server"; Url = "http://localhost:8761/actuator/health" }
)

$allHealthy = $true

foreach ($service in $services) {
    try {
        $response = Invoke-RestMethod -Uri $service.Url -Method Get

        if ($response.status -eq "UP") {
            Write-Host "[UP]   $($service.Name)" -ForegroundColor Green
        } else {
            Write-Host "[DOWN] $($service.Name) - Status: $($response.status)" -ForegroundColor Red
            $allHealthy = $false
        }
    } catch {
        Write-Host "[FAIL] $($service.Name) - $($_.Exception.Message)" -ForegroundColor Red
        $allHealthy = $false
    }
}

Write-Host ""

if ($allHealthy) {
    Write-Host "All services are healthy." -ForegroundColor Green
    exit 0
} else {
    Write-Host "Some services are not healthy." -ForegroundColor Red
    exit 1
}