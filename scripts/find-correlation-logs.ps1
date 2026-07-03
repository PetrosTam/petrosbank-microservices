[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateScript({
        -not [string]::IsNullOrWhiteSpace($_)
    })]
    [string]$CorrelationId,

    [ValidateRange(1, 100000)]
    [int]$Tail = 500,

    [switch]$ShowAll
)

$ErrorActionPreference = "Stop"

# ANSI formatting supported by the IntelliJ terminal.
$escape = [char]27
$bold = "${escape}[1m"
$brightRed = "${escape}[91m"
$reset = "${escape}[0m"

$CorrelationId = $CorrelationId.Trim()

function Invoke-DockerCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference

    $nativePreferenceExists =
        Test-Path variable:PSNativeCommandUseErrorActionPreference

    if ($nativePreferenceExists) {
        $previousNativePreference =
            $PSNativeCommandUseErrorActionPreference
    }

    try {
        # Native commands such as docker.exe may write to stderr.
        # Do not allow that output to terminate the PowerShell script.
        $ErrorActionPreference = "Continue"

        if ($nativePreferenceExists) {
            $PSNativeCommandUseErrorActionPreference = $false
        }

        $output = & docker @Arguments 2>&1
        $exitCode = $LASTEXITCODE

        $outputLines = @(
            $output |
                ForEach-Object {
                    $_.ToString()
                }
        )

        return [PSCustomObject]@{
            ExitCode = $exitCode
            Output   = $outputLines
        }
    }
    catch {
        return [PSCustomObject]@{
            ExitCode = 1
            Output   = @($_.Exception.Message)
        }
    }
    finally {
        $ErrorActionPreference =
            $previousErrorActionPreference

        if ($nativePreferenceExists) {
            $PSNativeCommandUseErrorActionPreference =
                $previousNativePreference
        }
    }
}

function Write-TechnicalFailure {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    Write-Host `
        "${brightRed}${bold}Log search failed.${reset}"

    Write-Host `
        "${brightRed}${Message}${reset}"
}

Write-Host ""

$title = "PetrosBank Microservices - Correlation ID Log Search"

Write-Host `
    "${bold}${title}${reset}" `
    -ForegroundColor Cyan

Write-Host `
    ("=" * $title.Length) `
    -ForegroundColor Cyan

Write-Host ""

Write-Host "Correlation ID: $CorrelationId"
Write-Host "Log tail size: $Tail"

$searchMode = if ($ShowAll) {
    "All matching logs"
}
else {
    "Important logs only"
}

Write-Host "Mode: $searchMode"
Write-Host ""

# Verify that the Docker CLI exists.
$dockerCommand =
    Get-Command docker -ErrorAction SilentlyContinue

if ($null -eq $dockerCommand) {
    Write-TechnicalFailure `
        -Message (
            "Docker CLI was not found. " +
            "Install Docker Desktop or add Docker to PATH."
        )

    exit 2
}

# Verify that the Docker Engine is running.
$dockerInfoResult =
    Invoke-DockerCommand `
        -Arguments @("info")

if ($dockerInfoResult.ExitCode -ne 0) {
    Write-TechnicalFailure `
        -Message (
            "Docker Engine is not available. " +
            "Start Docker Desktop and try again."
        )

    exit 2
}

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
    'no servers available|' +
    'access denied|' +
    'unauthorized|' +
    'forbidden|' +
    'conflict|' +
    'revoked|' +
    'rejected|' +
    'token expired|' +
    'status=5\d{2}|' +
    'failed|' +
    'warn(?:ing)?|' +
    'error|' +
    'exception'
)

$escapedCorrelationId =
    [regex]::Escape($CorrelationId)

$foundAny = $false

$failedContainers =
    [System.Collections.Generic.List[string]]::new()

foreach ($container in $containers) {
    Write-Host `
        "[$container]" `
        -ForegroundColor Yellow

    $logResult =
        Invoke-DockerCommand `
            -Arguments @(
                "logs",
                "--tail",
                $Tail.ToString(),
                $container
            )

    if ($logResult.ExitCode -ne 0) {
        $failedContainers.Add($container)

        Write-Host `
            "  Failed to read container logs." `
            -ForegroundColor Red

        Write-Host ""
        continue
    }

    $rawLogs = @($logResult.Output)

    $correlationMatches = @(
        $rawLogs |
            Select-String `
                -SimpleMatch `
                -Pattern $CorrelationId
    )

    if (-not $ShowAll) {
        $correlationMatches = @(
            $correlationMatches |
                Where-Object {
                    $_.Line -match $importantPattern
                }
        )
    }

    if ($correlationMatches.Count -eq 0) {
        Write-Host `
            "  No relevant logs found." `
            -ForegroundColor DarkGray

        Write-Host ""
        continue
    }

    $foundAny = $true

    foreach ($match in $correlationMatches) {
        $line = $match.Line

        # Keep only the log message after the logger separator.
        if ($line -match '\s-\s(?<message>.*)$') {
            $message = $Matches["message"]
        }
        else {
            $message = $line
        }

        # The correlation ID is already displayed at the top,
        # so remove its repeated appearance from each message.
        $message = $message -replace `
            "(?i)correlationId=$escapedCorrelationId,?\s*", ""

        Write-Host "  $message"
    }

    Write-Host ""
}

if ($failedContainers.Count -gt 0) {
    $failedContainerNames =
        $failedContainers -join ", "

    Write-Host `
        "${brightRed}${bold}Search completed with errors.${reset}"

    Write-Host `
        "${brightRed}Logs could not be read from: $failedContainerNames.${reset}"

    Write-Host `
        "${brightRed}The displayed search results may be incomplete.${reset}"

    exit 2
}

if ($foundAny) {
    Write-Host `
        "Search completed. Relevant logs found." `
        -ForegroundColor Green

    exit 0
}

Write-Host `
    "Search completed. No relevant logs found." `
    -ForegroundColor Yellow

exit 1