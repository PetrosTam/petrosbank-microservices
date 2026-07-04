# PetrosBank Microservices Troubleshooting Guide

This guide provides practical steps for diagnosing common problems in the PetrosBank microservices environment.

It covers:

- Docker container failures
- Spring Boot startup problems
- Correlation ID tracing
- API Gateway and Eureka discovery errors
- PostgreSQL connectivity
- Kafka and Zookeeper problems
- JWT authentication failures
- Container rebuilds and restarts

Run all commands from the project root unless stated otherwise.

---

## 1. Quick Diagnostic Workflow

When a request fails:

1. Check whether all containers are running.
2. Record the request correlation ID.
3. Run the correlation ID log-search script.
4. Identify the service where the failure occurred.
5. Inspect that service's complete logs.
6. Check for stack traces, connection failures, or service-discovery errors.
7. Restart or rebuild only the affected service when possible.

Start with:

```powershell
docker compose ps
```

Then search by correlation ID:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\find-correlation-logs.ps1 `
    -CorrelationId "your-complete-correlation-id" `
    -Tail 2000
```

Use `-ShowAll` when framework or diagnostic logs are required:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\find-correlation-logs.ps1 `
    -CorrelationId "your-complete-correlation-id" `
    -Tail 2000 `
    -ShowAll
```

---

## 2. Check Container Status

Display all services:

```powershell
docker compose ps
```

A healthy environment should normally show the required containers as running.

Check the state of a specific container:

```powershell
docker inspect user-service `
    --format 'Status={{.State.Status}} Restarting={{.State.Restarting}} ExitCode={{.State.ExitCode}} RestartCount={{.RestartCount}}'
```

Replace `user-service` with another container name when required.

Examples:

```text
api-gateway
user-service
account-service
transaction-service
notification-service
eureka-server
```

A non-zero exit code usually means that the application terminated during startup or runtime.

A growing restart count indicates that the container may be repeatedly crashing.

Check the configured health status:

```powershell
docker inspect user-service `
    --format '{{json .State.Health}}'
```

Some containers may not have a Docker health check configured. In that case, inspect their logs and Spring Boot health endpoint instead.

---

## 3. View Complete Container Logs

View the latest logs from a service:

```powershell
docker logs user-service --tail 500
```

View logs generated during the last ten minutes:

```powershell
docker logs user-service --since 10m
```

Follow logs in real time:

```powershell
docker logs user-service --follow --tail 100
```

Press `Ctrl + C` to stop following the logs.

The Docker Compose equivalent is:

```powershell
docker compose logs --tail 500 user-service
```

Follow logs through Docker Compose:

```powershell
docker compose logs --follow --tail 100 user-service
```

---

## 4. Search Logs by Correlation ID

The recommended approach is the project utility:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\find-correlation-logs.ps1 `
    -CorrelationId "your-complete-correlation-id"
```

The correlation ID must be supplied exactly. Characters such as `...` are treated literally and are not wildcards.

To inspect surrounding lines directly:

```powershell
$correlationId = "your-complete-correlation-id"

docker logs user-service --tail 2000 2>&1 |
    Select-String `
        -Pattern ([regex]::Escape($correlationId)) `
        -Context 10,20
```

This displays:

```text
10 lines before each match
20 lines after each match
```

Surrounding context is useful when an exception stack trace does not repeat the correlation ID on every line.

---

## 5. Search for Errors and Exceptions

Search one service for common failure patterns:

```powershell
docker logs user-service --tail 2000 2>&1 |
    Select-String `
        -Pattern "ERROR|WARN|Exception|Caused by|failed|status=5\d{2}"
```

Search all application services:

```powershell
$containers = @(
    "api-gateway",
    "user-service",
    "account-service",
    "transaction-service",
    "notification-service"
)

foreach ($container in $containers) {
    Write-Host ""
    Write-Host "[$container]" -ForegroundColor Yellow

    docker logs $container --tail 1000 2>&1 |
        Select-String `
            -Pattern "ERROR|WARN|Exception|Caused by|failed|status=5\d{2}"
}
```

Warnings do not always indicate a real application failure. Always examine the surrounding messages and final HTTP status.

---

## 6. Spring Boot Startup Failures

Common startup failure messages include:

```text
APPLICATION FAILED TO START
BeanCreationException
UnsatisfiedDependencyException
Connection refused
Port already in use
Failed to configure a DataSource
```

Inspect the beginning and end of the affected service logs:

```powershell
docker logs user-service --tail 1000
```

Check the container exit code:

```powershell
docker inspect user-service `
    --format 'ExitCode={{.State.ExitCode}} Error={{.State.Error}}'
```

Common causes include:

- Missing environment variables
- Invalid Spring configuration
- Database connectivity failures
- Kafka or Eureka being unavailable
- Port conflicts
- Compilation or dependency errors
- Incorrect Docker image configuration

After fixing the issue, rebuild only the affected service:

```powershell
docker compose up -d --build user-service
```

---

## 7. API Gateway and Eureka 503 Errors

A temporary Gateway response such as:

```text
503 Service Unavailable
No servers available for service
```

usually means that the target service has not yet registered with Eureka or the Gateway has not refreshed its discovery information.

Check:

```powershell
docker compose ps
docker logs api-gateway --tail 500
docker logs eureka-server --tail 500
docker logs user-service --tail 500
```

Replace `user-service` with the service involved in the failed route.

Confirm that:

- Eureka Server is running
- The target service started successfully
- The target service registered with Eureka
- The API Gateway can reach Eureka
- The service name matches the Gateway route configuration

A small number of `503` responses during initial startup can be temporary. Repeated `503` responses after all services are ready require investigation.

Restart the affected components when necessary:

```powershell
docker compose restart eureka-server
docker compose restart user-service
docker compose restart api-gateway
```

Restart dependencies before the Gateway whenever possible.

---

## 8. PostgreSQL Connection Problems

Common database errors include:

```text
Connection refused
Could not open JDBC Connection
Failed to obtain JDBC Connection
password authentication failed
database does not exist
relation does not exist
```

First check the containers:

```powershell
docker compose ps
```

Then inspect the PostgreSQL logs using the container name shown by `docker compose ps`:

```powershell
docker logs <postgres-container-name> --tail 500
```

Inspect the affected Spring Boot service:

```powershell
docker logs user-service --tail 1000
```

Verify:

- PostgreSQL is running
- The database hostname matches the Docker Compose service name
- Database name, username, and password are correct
- The Spring datasource URL uses the internal Docker network hostname
- Database migrations or schema initialization completed
- The application did not start before PostgreSQL became ready

Restart the database and affected service:

```powershell
docker compose restart <postgres-service-name>
docker compose restart user-service
```

Do not use `docker compose down -v` unless database volume deletion is intentional.

The `-v` option permanently removes Compose-managed volumes and may delete local development data.

---

## 9. Kafka and Zookeeper Problems

Common Kafka errors include:

```text
Connection to node could not be established
Broker may not be available
TimeoutException
Topic does not exist
Failed to send message
Bootstrap broker disconnected
```

Check the relevant containers:

```powershell
docker compose ps
```

Inspect Kafka and Zookeeper logs:

```powershell
docker logs <kafka-container-name> --tail 500
docker logs <zookeeper-container-name> --tail 500
```

Inspect the application service producing or consuming messages:

```powershell
docker logs notification-service --tail 1000
```

Verify:

- Kafka and Zookeeper are running
- The Kafka bootstrap-server hostname matches the Docker Compose service name
- Topics are created or auto-creation is enabled
- Producers and consumers use compatible serializers
- The application service can reach Kafka through the Docker network

Restart dependencies first:

```powershell
docker compose restart <zookeeper-service-name>
docker compose restart <kafka-service-name>
docker compose restart notification-service
```

Some Kafka warnings may appear during initial startup while the broker becomes available.

---

## 10. JWT and Authentication Problems

Typical authentication responses include:

```text
401 Unauthorized
403 Forbidden
Access token expired
Invalid token
Refresh token revoked
Refresh token rejected
```

Check Gateway and User Service logs:

```powershell
docker logs api-gateway --tail 1000
docker logs user-service --tail 1000
```

Search using the request correlation ID:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\find-correlation-logs.ps1 `
    -CorrelationId "your-complete-correlation-id" `
    -Tail 2000
```

Verify:

- The header uses `Authorization: Bearer <access-token>`
- The access token has not expired
- The signing secret is consistent between token creation and validation
- The expected roles or authorities are present
- The refresh token has not expired
- The refresh token has not already been revoked by logout

A `401` response after attempting to reuse a refresh token that was revoked during logout is expected behavior.

Do not include access tokens, refresh tokens, passwords, or secrets in committed diagnostic files.

---

## 11. Test Spring Boot Health Endpoints

When Actuator health endpoints are exposed, test a service directly:

```powershell
Invoke-RestMethod `
    -Uri "http://localhost:<service-port>/actuator/health"
```

Expected healthy output is similar to:

```json
{
  "status": "UP"
}
```

A service may be running as a Docker container but still not be ready to process requests. Health checks help distinguish container availability from application readiness.

Use the project's health-check script for the full environment:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\health-check.ps1
```

---

## 12. Restart or Rebuild a Service

Restart without rebuilding:

```powershell
docker compose restart user-service
```

Rebuild and recreate one service:

```powershell
docker compose up -d --build user-service
```

Rebuild multiple services:

```powershell
docker compose up -d --build `
    api-gateway `
    user-service `
    account-service
```

Recreate the complete environment:

```powershell
docker compose down
docker compose up -d --build
```

Avoid adding `-v` to `docker compose down` unless stored database data should be deleted.

---

## 13. Verify the Environment After a Fix

Check all containers:

```powershell
docker compose ps
```

Run the health-check script:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\health-check.ps1
```

Run the API smoke test:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\api-smoke-test.ps1
```

Use the correlation ID printed by the smoke test:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\find-correlation-logs.ps1 `
    -CorrelationId "correlation-id-from-smoke-test" `
    -Tail 2000
```

---

## 14. Collect Diagnostic Information

Before investigating a difficult problem, record:

- The failing request and HTTP status
- The complete correlation ID
- The affected service
- Container status
- Relevant log lines
- Exception stack trace
- Container restart count
- Recent configuration or code changes

Create a local diagnostics directory:

```powershell
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$diagnosticsDirectory = ".\diagnostics\$timestamp"

New-Item `
    -ItemType Directory `
    -Force `
    -Path $diagnosticsDirectory |
    Out-Null
```

Save container status:

```powershell
docker compose ps 2>&1 |
    Out-File "$diagnosticsDirectory\docker-compose-ps.txt"
```

Save recent logs:

```powershell
docker compose logs --tail 1000 2>&1 |
    Out-File "$diagnosticsDirectory\docker-compose-logs.txt"
```

The `diagnostics` directory should not be committed if it may contain tokens, email addresses, account information, or other sensitive data.

---

## 15. Useful Command Summary

```powershell
# Check all containers
docker compose ps

# View recent service logs
docker logs user-service --tail 500

# Follow service logs
docker logs user-service --follow --tail 100

# View logs from the last ten minutes
docker logs user-service --since 10m

# Inspect container status and restart count
docker inspect user-service `
    --format 'Status={{.State.Status}} ExitCode={{.State.ExitCode}} RestartCount={{.RestartCount}}'

# Restart one service
docker compose restart user-service

# Rebuild one service
docker compose up -d --build user-service

# Run health checks
powershell -ExecutionPolicy Bypass `
    -File .\scripts\health-check.ps1

# Run API smoke tests
powershell -ExecutionPolicy Bypass `
    -File .\scripts\api-smoke-test.ps1

# Search distributed logs by correlation ID
powershell -ExecutionPolicy Bypass `
    -File .\scripts\find-correlation-logs.ps1 `
    -CorrelationId "your-complete-correlation-id" `
    -Tail 2000
```