param(
    [Parameter(Mandatory = $true)]
    [string]$CorrelationId,

    [int]$Tail = 500,

    [switch]$ShowAll
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "Banking App - Correlation ID Log Search" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Correlation ID: $CorrelationId"
Write-Host "Log tail size: $Tail"
Write-Host "Mode: $(if ($ShowAll) { 'All matching logs' } else { 'Important logs only' })"
Write-Host ""

$containers = @(
    "api-gateway",
    "user-service",
    "account-service",
    "transaction-service",
    "notification-service"
)

$importantPattern = '(?i)' + (
    'request received|' +
    'request completed|' +
    'registration request|' +
    'login request|' +
    'refresh token request|' +
    'logout request|' +
    'account created|' +
    'deposit|' +
    'withdrawal|' +
    'transfer|' +
    'insufficient funds|' +
    'access denied|' +
    'failed|' +
    'warning|' +
    'error|' +
    'exception'
)

$foundAny = $false

foreach ($container in $containers) {
    Write-Host "[$container]" -ForegroundColor Yellow

    try {
        $correlationMatches = docker logs $container --tail $Tail 2>&1 |
                Select-String -Pattern ([regex]::Escape($CorrelationId))

        if (-not $ShowAll) {
            $correlationMatches = $correlationMatches |
                    Where-Object { $_.Line -match $importantPattern }
        }

        if ($correlationMatches) {
            $foundAny = $true

            foreach ($match in $correlationMatches) {
                $line = $match.Line

                # Keep only the actual log message after the logger separator.
                if ($line -match '\s-\s(?<message>.*)$') {
                    $message = $Matches["message"]
                } else {
                    $message = $line
                }

                # The correlation ID is already displayed at the top,
                # so remove its repeated appearance from every line.
                $escapedCorrelationId = [regex]::Escape($CorrelationId)

                $message = $message -replace `
                    "correlationId=$escapedCorrelationId,?\s*", ""

                Write-Host "  $message"
            }
        } else {
            Write-Host "  No relevant logs found." -ForegroundColor DarkGray
        }
    }
    catch {
        Write-Host "  Failed to read logs: $($_.Exception.Message)" `
            -ForegroundColor Red
    }

    Write-Host ""
}

if ($foundAny) {
    Write-Host "Search completed. Relevant logs found." -ForegroundColor Green
    exit 0
}

Write-Host "Search completed. No relevant logs found." -ForegroundColor Yellow
exit 1