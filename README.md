# PetrosBank Microservices

A Spring Boot banking microservices project built with Java 17, Docker, PostgreSQL, Kafka, Eureka Service Discovery, API Gateway routing, JWT authentication, refresh tokens, structured error responses, correlation ID tracing, request duration logging, and Spring Boot Actuator observability.

This project is intended as a backend engineering portfolio project focused on enterprise-style banking application patterns.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Services](#services)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Running the Project](#running-the-project)
- [Environment Variables](#environment-variables)
- [API Gateway Routes](#api-gateway-routes)
- [Authentication Flow](#authentication-flow)
- [Observability](#observability)
- [Actuator Endpoints](#actuator-endpoints)
- [Local Health Check Script](#local-health-check-script)
- [API Smoke Test Script](#api-smoke-test-script)
- [Correlation ID Log Search](#correlation-id-log-search)
- [Example API Flow](#example-api-flow)
- [Project Improvements Implemented](#project-improvements-implemented)
- [Future Improvements](#future-improvements)
- [License](#license)

---

## Overview

This project demonstrates a modular banking backend using a microservices architecture.

The system includes separate services for users, accounts, transactions, notifications, service discovery, and API gateway routing. Each main business service owns its own PostgreSQL database and communicates through REST APIs and Kafka events.

The project includes practical enterprise backend features such as:

- Central API Gateway entry point
- Eureka service discovery
- JWT authentication
- Refresh token authentication flow
- Role-based endpoint protection
- Per-service PostgreSQL databases
- Kafka-based event communication
- Correlation ID propagation
- Request and response tracing logs
- Request duration logging
- Consistent structured error responses
- Spring Boot Actuator health and info metadata
- Docker Compose local infrastructure

---

## Architecture

![System Architecture](./banking-app-architecture.png)

---

## Services

| Service | Port | Responsibility |
|---|---:|---|
| API Gateway | `8080` | Central entry point and route forwarding |
| User Service | `8081` | Registration, login, JWT issuance, refresh tokens, user profiles |
| Account Service | `8082` | Account creation, account limits, account status, balance ownership |
| Transaction Service | `8083` | Deposits, withdrawals, transfers, balance lookup, transaction history |
| Notification Service | `8084` | Consumes Kafka events and prepares notification workflows |
| Eureka Server | `8761` | Service registry and discovery |
| Kafka | `9092` / internal | Event streaming |
| PostgreSQL Databases | Docker internal | Separate database per service |

---

## Key Features

### API Gateway

All external API requests can go through the API Gateway:

```text
http://localhost:8080
```

Gateway routes:

```text
/api/v1/users/**          -> user-service
/api/v1/accounts/**       -> account-service
/api/v1/transactions/**   -> transaction-service
```

### Authentication

The User Service supports:

- User registration
- Login with email and password
- JWT access token generation
- Refresh token creation
- Refresh token validation
- Logout by revoking refresh tokens

Protected endpoints require:

```http
Authorization: Bearer <access-token>
```

### Correlation ID Tracing

The system supports request tracing with:

```http
X-Correlation-ID
```

If the client does not provide a correlation ID, the API Gateway or service creates one automatically.

The same correlation ID is included in:

- Request logs
- Response headers
- Error responses
- Service logs through MDC

Example log:

```text
Gateway request completed. correlationId=petros-test-id, method=GET, path=/api/v1/users/accessAll, status=200, durationMs=23
```

### Structured Error Responses

Error responses include useful debugging metadata:

```json
{
  "errorCode": "NOT_FOUND",
  "errorMessage": "Account number does not exist: 9999999999",
  "errorDetails": "Account not found. Invalid Account.",
  "timestamp": "2026-06-29T15:46:44.018Z",
  "path": "/api/v1/transactions/balance/9999999999",
  "correlationId": "petros-transaction-duration-test"
}
```

### Request Duration Logging

Each main service logs when a request starts and when it completes.

Example:

```text
Transaction service request received. correlationId=abc-123, method=GET, path=/api/v1/transactions/balance/9999999999
Transaction service request completed. correlationId=abc-123, method=GET, path=/api/v1/transactions/balance/9999999999, status=404, durationMs=237
```

### Actuator Metadata

Each service exposes professional metadata through `/actuator/info`.

Example:

```json
{
  "app": {
    "name": "Transaction Service",
    "version": "1.0.0",
    "description": "Service that processes deposits, withdrawals, transfers, balance checks, and transaction history"
  },
  "technology": {
    "java": 17,
    "framework": "Spring Boot",
    "architecture": "Microservices"
  },
  "domain": {
    "bounded-context": "Transaction Processing",
    "main-responsibilities": "Deposits, withdrawals, transfers, balance lookup, transaction history, and account validation"
  },
  "messaging": {
    "kafka-topic": "transaction-events"
  },
  "build": {
    "artifact": "transaction-service",
    "name": "transaction-service",
    "version": "0.0.1-SNAPSHOT",
    "group": "com.backendev"
  }
}
```

---

## Tech Stack

### Backend

- Java 17
- Spring Boot
- Spring Security
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka
- Spring Data JPA
- Hibernate
- Maven

### Authentication

- JWT access tokens
- Refresh tokens
- Role-based access control

### Data

- PostgreSQL
- Database-per-service pattern

### Messaging

- Apache Kafka
- Zookeeper
- Spring Kafka

### Infrastructure

- Docker
- Docker Compose
- Eureka Service Discovery
- API Gateway

### Observability

- Spring Boot Actuator
- Health endpoints
- Info metadata
- Correlation ID logging
- Request duration logging
- Structured error responses

---

## Running the Project

### Prerequisites

- Java 17+
- Docker Desktop
- Docker Compose
- Git

### Clone the repository

```bash
git clone https://github.com/PetrosTam/petrosbank-microservices.git
cd petrosbank-microservices
```

### Create `.env`

Create a `.env` file in the project root:

```env
JWT_SECRET=your-256-bit-secret
DB_USER=postgres
DB_PASS=postgres
MAIL_USERNAME=dummy@gmail.com
MAIL_PASSWORD=dummy
```

For local development, dummy mail credentials are acceptable because the notification service disables mail health checks.

### Start all services

```bash
docker compose up -d --build
```

### Check running containers

```bash
docker compose ps
```

### Stop all services

```bash
docker compose down
```

---

## Environment Variables

An example environment file is provided as `.env.example`.

For local setup, copy it to `.env` and update the values if needed:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

The real `.env` file should not be committed to Git because it may contain secrets such as JWT keys, database passwords, or mail credentials.

| Variable | Description | Example |
|---|---|---|
| `JWT_SECRET` | Secret key used for JWT signing | `your-secret-key` |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | Access token lifetime in milliseconds | `900000` |
| `DB_USER` | PostgreSQL username | `postgres` |
| `DB_PASS` | PostgreSQL password | `postgres` |
| `MAIL_USERNAME` | SMTP username for notification service | `dummy@gmail.com` |
| `MAIL_PASSWORD` | SMTP password or app password | `dummy` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap server | `kafka:29092` |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | Eureka registry URL | `http://eureka-server:8761/eureka/` |

---

## API Gateway Routes

Base URL:

```text
http://localhost:8080
```

| Route | Target Service |
|---|---|
| `/api/v1/users/**` | User Service |
| `/api/v1/accounts/**` | Account Service |
| `/api/v1/transactions/**` | Transaction Service |

Example public endpoint:

```bash
curl http://localhost:8080/api/v1/users/accessAll
```

---

## Authentication Flow

### 1. Register user

```http
POST /api/v1/users/register
```

Example body:

```json
{
  "firstName": "Petros",
  "lastName": "Test",
  "email": "petros@test.com",
  "password": "Password123!",
  "phone": "99999999",
  "roles": ["ROLE_USER"]
}
```

### 2. Login

```http
POST /api/v1/users/login
```

Example body:

```json
{
  "email": "petros@test.com",
  "password": "Password123!"
}
```

Example response:

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "email": "petros@test.com",
  "roles": ["ROLE_USER"],
  "expiration": "..."
}
```

### 3. Use access token

Include the access token in the `Authorization` header of protected requests:

```http
Authorization: Bearer <access-token>
```

### 4. Refresh access token

```http
POST /api/v1/users/refresh-token
```

Example body:

```json
{
  "refreshToken": "<refresh-token>"
}
```

A valid refresh token returns a new access token.

### 5. Logout

```http
POST /api/v1/users/logout
```

Example body:

```json
{
  "refreshToken": "<refresh-token>"
}
```

Expected response:

```text
HTTP 204 No Content
```

Logout revokes the supplied refresh token. Attempting to reuse the same refresh token after logout returns:

```text
HTTP 401 Unauthorized
```

### Token Lifetime and Logout Behavior

Access tokens expire after 15 minutes by default. The lifetime can be configured through the `JWT_ACCESS_TOKEN_EXPIRATION_MS` environment variable.

Each access token includes a unique JWT ID (`jti`) to support future token revocation and blacklist mechanisms.

Logout revokes the user's refresh token. An access token that has already been issued remains valid until its configured expiration time.

---

## Observability

### Correlation ID

Send a custom correlation ID:

```powershell
$customCorrelationId = "petros-test-correlation-id"

Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/users/accessAll" `
    -Method Get `
    -Headers @{ "X-Correlation-ID" = $customCorrelationId }
```

Check logs:

```bash
docker logs api-gateway --tail 100
docker logs user-service --tail 100
docker logs account-service --tail 100
docker logs transaction-service --tail 100
```

### Clean production-style logs

The services are configured to reduce unnecessary Spring DEBUG logs and focus on application-level request tracing.

---

## Actuator Endpoints

### API Gateway

```text
http://localhost:8080/actuator/health
http://localhost:8080/actuator/info
```

### User Service

```text
http://localhost:8081/actuator/health
http://localhost:8081/actuator/info
```

### Account Service

```text
http://localhost:8082/actuator/health
http://localhost:8082/actuator/info
```

### Transaction Service

```text
http://localhost:8083/actuator/health
http://localhost:8083/actuator/info
```

### Notification Service

```text
http://localhost:8084/actuator/health
http://localhost:8084/actuator/info
```

PowerShell pretty JSON:

```powershell
Invoke-RestMethod -Uri "http://localhost:8084/actuator/info" -Method Get | ConvertTo-Json -Depth 10
```

---

## Local Health Check Script

A PowerShell health-check script is included to verify that all core services are running locally and ready to receive requests.

Run the script from the project root:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\health-check.ps1
```

The script checks the following Spring Boot Actuator health endpoints:

| Service | Health Endpoint |
|---|---|
| API Gateway | `http://localhost:8080/actuator/health` |
| User Service | `http://localhost:8081/actuator/health` |
| Account Service | `http://localhost:8082/actuator/health` |
| Transaction Service | `http://localhost:8083/actuator/health` |
| Notification Service | `http://localhost:8084/actuator/health` |
| Eureka Server | `http://localhost:8761/actuator/health` |

### Round-Based Retry Handling

The script uses round-based retries.

During each attempt, it checks all services that are still unavailable. Services that become healthy are removed from subsequent retry rounds.

This is more efficient than exhausting all retries for one service before checking the next service, and it gives the complete microservices environment enough time to recover during a Docker cold start.

Default retry policy:

```text
6 total attempts per service
1 initial attempt and 5 retries
Retry delays: 5s, 10s, 15s, 15s, 15s
Maximum retry delay: 15s
```

A live countdown spinner is displayed while the script waits for the next retry round.

Example cold-start recovery output (abbreviated):

```text
PetrosBank Microservices - Local Health Check
=============================================

Retry policy: 6 total attempts per service (1 initial attempt + 5 retries; delays: 5s, 10s, 15s, 15s, 15s).

Attempt 5/6
-----------

[UP]     API Gateway
[FAILED] User Service
[FAILED] Account Service
[FAILED] Transaction Service
[UP]     Notification Service
[UP]     Eureka Server

3/6 services recovered; retrying 3 unhealthy services...
```

Example successful output:

```text
PetrosBank Microservices - Local Health Check
=============================================

Retry policy: 6 total attempts per service (1 initial attempt + 5 retries; delays: 5s, 10s, 15s, 15s, 15s).

Attempt 1/6
-----------

[UP]     API Gateway
[UP]     User Service
[UP]     Account Service
[UP]     Transaction Service
[UP]     Notification Service
[UP]     Eureka Server

All services are healthy.
```

### Exit Codes

```text
0 = All services are healthy
1 = One or more services remain unhealthy
```

The exit codes allow the script to be used in local automation and future CI/CD pipelines.

### Custom Configuration

The default retry and timeout values can be overridden through command-line parameters:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\health-check.ps1 `
    -MaxAttempts 4 `
    -InitialDelaySeconds 3 `
    -MaxDelaySeconds 10 `
    -RequestTimeoutSeconds 5
```

This script is useful after starting Docker Desktop or Docker Compose to confirm that the complete local microservices environment is available.

---

## API Smoke Test Script

A PowerShell API smoke-test script is included to verify that the main application flow works end-to-end through the API Gateway.

Run the script from the project root:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\api-smoke-test.ps1
```

The script generates a unique test user and uses one correlation ID throughout the complete execution.

### Component Readiness Check

Before running the API flow, the script verifies that the following required components respond with an `UP` health status:

| Component | Health Endpoint |
|---|---|
| API Gateway | `http://localhost:8080/actuator/health` |
| User Service | `http://localhost:8081/actuator/health` |
| Account Service | `http://localhost:8082/actuator/health` |
| Eureka Server | `http://localhost:8761/actuator/health` |

The component checks use round-based retries. Components that become ready are removed from later retry attempts.

Default component-readiness policy:

```text
6 total attempts
1 initial attempt and 5 retries
Retry delays: 5s, 10s, 15s, 15s, 15s
Maximum retry delay: 15s
```

### Gateway Route Readiness Check

A healthy API Gateway does not necessarily mean that Eureka service discovery and downstream routing are already ready.

After the component checks pass, the script verifies that the API Gateway can successfully route a real request to the User Service:

```http
GET /api/v1/users/accessAll
```

Default Gateway route-readiness policy:

```text
5 total attempts
1 initial attempt and 4 retries
Retry delays: 3s, 6s, 10s, 10s
Maximum retry delay: 10s
```

This prevents temporary Eureka registration or API Gateway discovery delays from causing an immediate HTTP `503 Service Unavailable` failure.

### End-to-End API Flow

After component and route readiness are confirmed, the script tests the following flow:

| Step | What It Tests |
|---|---|
| 1 | Public endpoint through the API Gateway |
| 2 | User registration |
| 3 | Login and access-token and refresh-token issuance |
| 4 | Access-token refresh |
| 5 | Account creation through the API Gateway |
| 6 | Logout and refresh-token revocation |
| 7 | Rejection of the revoked refresh token with HTTP 401 |

### Transient Request Handling

Temporary failures are retried only for requests that are safe to repeat.

The script recognises the following failures as transient:

```text
HTTP 502 Bad Gateway
HTTP 503 Service Unavailable
HTTP 504 Gateway Timeout
Connection or transport failures
```

Default transient-request policy:

```text
3 total attempts
1 initial attempt and 2 retries
Retry delays: 2s, 4s
```

Non-idempotent resource-creation operations, specifically user registration and account creation, are not automatically retried. This avoids duplicate resources if a request succeeds but its response is lost.

Logout is also not automatically retried because the first request may already have revoked the refresh token even if its response was not received.

### Correlation ID

A unique correlation ID is generated for every smoke-test execution and sent with the Gateway route-readiness request and all end-to-end API requests.

Example:

```text
Correlation ID: petros-api-smoke-test-4327316d-7b71-463c-8cbd-3ae97c4585bf
```

The correlation ID can be used with the log-search script to locate the related request flow across the API Gateway and downstream services.

### Example Successful Output (Abbreviated)

```text
PetrosBank Microservices - API Smoke Test
=========================================

Component readiness check
-------------------------

Attempt 1/6
-----------

[UP]     API Gateway
[UP]     User Service
[UP]     Account Service
[UP]     Eureka Server

All required components are ready.

Gateway route readiness check
-----------------------------

Attempt 1/5
-----------

[UP]     API Gateway -> User Service route

Gateway routing is ready.

[1/7] Testing public gateway endpoint...
[OK] Public endpoint response: This endpoint can be accessed by all the users!

[2/7] Registering test user...
[OK] Registered user: smoke-test-example@test.com

[3/7] Logging in...
[OK] Login successful. Access token and refresh token received.

[4/7] Refreshing access token...
[OK] Refresh token flow works.

[5/7] Creating account through API Gateway...
[OK] Account created successfully.
Account Number: 1508912413

[6/7] Logging out and revoking refresh token...
[OK] Logout successful. Refresh token revoked.

[7/7] Verifying revoked refresh token is rejected...
[OK] Revoked refresh token correctly rejected with HTTP 401.

API smoke test completed successfully.
```

### Exit Codes

```text
0 = Complete smoke test passed
1 = Readiness check or API test failed
```

The exit codes allow the script to be used in local automation and future CI/CD pipelines.

### Custom Configuration

The API Gateway base URL, retry policies, and request timeout can be overridden through command-line parameters:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\api-smoke-test.ps1 `
    -BaseUrl "http://localhost:8080" `
    -ReadinessMaxAttempts 6 `
    -RouteReadinessMaxAttempts 5 `
    -TransientRequestMaxAttempts 3 `
    -RequestTimeoutSeconds 10
```

This script is useful after starting the Docker Compose environment to confirm that component readiness, Eureka service discovery, API Gateway routing, authentication, token refresh, account creation, logout, and refresh-token revocation work end-to-end.

---

## Correlation ID Log Search

A PowerShell utility is included for tracing a request across the microservices by using its correlation ID.

Run the script from the project root:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\find-correlation-logs.ps1 `
    -CorrelationId "your-complete-correlation-id"
```

The complete correlation ID must be supplied exactly as it appears in the API response or smoke-test output. Ellipses such as `...` are treated as literal characters and are not wildcards.

Example:

```text
petros-api-smoke-test-ab510a4a-4d38-4a69-b4bc-493e34d98517
```

### Important Logs Mode

By default, the script displays only the most relevant application logs, including:

- Request start and completion
- HTTP method, path, status, and duration
- User registration and login activity
- Refresh-token and logout operations
- Account creation
- Deposits, withdrawals, and transfers
- Insufficient-funds events
- Service-discovery failures
- Token revocation and rejection
- Access-denied and authorization failures
- Warnings, errors, exceptions, and HTTP 5xx responses

Example output:

```text
PetrosBank Microservices - Correlation ID Log Search
====================================================

Correlation ID: petros-api-smoke-test-example
Log tail size: 2000
Mode: Important logs only

[api-gateway]
  Gateway request received. method=POST, path=/api/v1/users/register
  Gateway request completed. method=POST, path=/api/v1/users/register, status=201, durationMs=130

[user-service]
  User service request received. method=POST, path=/api/v1/users/register
  Registration request received for email=smoke-test-example@test.com
  User service request completed. method=POST, path=/api/v1/users/register, status=201, durationMs=124

[account-service]
  Account service request received. method=POST, path=/api/v1/accounts
  Account created for user with user Id 6
  Account service request completed. method=POST, path=/api/v1/accounts, status=201, durationMs=60

[transaction-service]
  No relevant logs found.

[notification-service]
  No relevant logs found.

Search completed. Relevant logs found.
```

A service may display `No relevant logs found` when the traced request flow did not call that service.

### Show All Matching Logs

To display every matching log line, including framework, Kafka, and diagnostic logs, use the `-ShowAll` option:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\find-correlation-logs.ps1 `
    -CorrelationId "your-complete-correlation-id" `
    -ShowAll
```

### Log Tail Size

The optional `-Tail` parameter controls how many recent lines are searched in each container.

The default value is `500`:

```powershell
powershell -ExecutionPolicy Bypass `
    -File .\scripts\find-correlation-logs.ps1 `
    -CorrelationId "your-complete-correlation-id" `
    -Tail 2000
```

A larger value can be useful when many logs have been generated since the traced request was executed.

### Docker Validation

Before searching the logs, the script verifies that:

- The Docker CLI is installed and available in `PATH`
- The Docker Engine is running
- The configured containers can be accessed

If Docker Desktop is not running, the script returns a clean error:

```text
Log search failed.
Docker Engine is not available. Start Docker Desktop and try again.
```

If logs cannot be read from one or more containers, the script reports that the displayed results may be incomplete.

### Searched Containers

The script searches the following containers:

- API Gateway
- User Service
- Account Service
- Transaction Service
- Notification Service

### Exit Codes

```text
0 = Matching logs were found
1 = The search completed successfully, but no matching logs were found
2 = Docker or one or more containers could not be accessed
```

These exit codes allow the script to distinguish between an empty search result and a technical failure.

Docker container logs remain available for deeper debugging. The script only provides a filtered view and does not modify or delete the original logs.

### Deeper Debugging

The correlation log-search script provides a filtered view of the request flow. When additional context is required, the complete Docker container logs remain available.

View the latest logs for a specific service:

```powershell
docker logs user-service --tail 500
```

Follow logs in real time while executing a request from another terminal:

```powershell
docker logs user-service --follow --tail 100
```

Search for a correlation ID together with the surrounding log lines:

```powershell
$correlationId = "your-complete-correlation-id"

docker logs user-service --tail 2000 2>&1 |
    Select-String `
        -Pattern ([regex]::Escape($correlationId)) `
        -Context 10,20
```

View warnings, errors, exceptions, and server failures:

```powershell
docker logs user-service --tail 2000 2>&1 |
    Select-String `
        -Pattern "ERROR|WARN|Exception|Caused by|failed|status=5\d{2}"
```

Check the current state of all containers:

```powershell
docker compose ps
```

The log-search script does not modify or delete the original Docker logs.

---

## Example API Flow

A typical banking flow:

```text
1. Register user
2. Login and receive access token + refresh token
3. Create bank account
4. Deposit money
5. Withdraw money
6. Create second account
7. Transfer money between accounts
8. Fetch transaction history
9. Use correlation ID to trace logs across services
```

---

## Project Improvements Implemented

This project was extended and improved with the following backend engineering upgrades:

- Added API Gateway with Eureka-based routing
- Added refresh token authentication flow
- Added logout by refresh token revocation
- Fixed transaction history to include both incoming and outgoing transactions
- Improved account limit error handling with proper `409 CONFLICT`
- Added correlation ID propagation through API Gateway and services
- Added correlation ID to structured error responses
- Added timestamp, request path, and correlation ID to error responses
- Added request completion logging with HTTP status and duration
- Added clean production-style logging levels
- Added custom log patterns with service name and correlation ID
- Added Actuator health/info metadata for all services
- Added notification service actuator support
- Disabled mail health check for local development dummy SMTP credentials
- Added build metadata to notification service

---

## Future Improvements

Possible future improvements:

- Centralized logging with ELK or OpenSearch
- Distributed tracing with OpenTelemetry
- Metrics dashboards with Prometheus and Grafana
- More complete API documentation collection
- Integration tests for gateway-based flows
- Password reset flow
- Email verification flow
- CI/CD pipeline hardening
- Kubernetes deployment manifests
- Helm chart support
- Centralized configuration with Spring Cloud Config

---

## License

This project is licensed under the MIT License.