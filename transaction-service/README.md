# Transaction Service
The Transaction Service handles all monetary operations within the Banking App ecosystem, including deposits, withdrawals, transfers, and transaction history. It communicates with the Account Service to validate accounts and update balances securely.

### Features
- Deposit funds into a user account 
- Withdraw funds securely with balance validation 
- Transfer funds between two accounts 
- Retrieve transaction history for a specific account 
- Fetch current account balance via Account Service 
- External API calls to Account Service for validation and balance updates 
- Clean separation of transaction logic and account state management 
- OpenAPI documentation via Swagger UI 
- Dockerized for easy orchestration


### Technologies Used

- Java 17
- Spring Boot
- Spring Security (JWT-based)
- Spring Data JPA
- PostgreSQL
- Maven
- Docker
- Eureka Client (Service Discovery)

### Getting Started

### Prerequisites

- Java 17+
- Maven
- A running database (PostgreSQL, MySQL, or H2 for testing)

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/your-username/transaction-service.git
   cd transaction-service

2. **Configure application properties**

    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/transactiondb
        username: your-db-username
        password: your-db-password
        driver-class-name: org.postgresql.Driver
      jpa:
        hibernate:
          ddl-auto: update
        properties:
          hibernate:
            dialect: org.hibernate.dialect.PostgreSQLDialect
        show-sql: true
    ```

3. **Build the project**
    ```bash
    ./mvnw clean install
    ```

4. **Run the application**
    ```bash 
    ./mvnw spring-boot:run
    ```

### Docker setup
**Build and run the service**
```bash 
  docker-compose up --build transaction-service
```
- Exposes port 8083 
- Registers with Eureka at startup 
- Required environment variables:
  - JWT_SECRET 
  - EUREKA_SERVER_URL 
  - ACCOUNT_SERVICE_URL 
  - DB_URL, DB_USERNAME, DB_PASSWORD

### API Endpoints

| Method | Endpoint                                       | Description                          |
|--------|------------------------------------------------|--------------------------------------|
| POST   | `/api/v1/transaction/deposit`                  | Deposit money into an account        |
| POST   | `/api/v1/transactions/withdraw`                | Withdraw money from an account       |
| POST   | `/api/v1/transactions/transfer`                | Transfer money between accounts      |
| GET    | `/api/v1/transactions/account/{accountNumber}` | Fetch transactions for an account    |
| GET    | `/api/v1/transactions/balance/{accountNumber}` | Fetch current balance of an account  |


> JWT must be included in the `Authorization` header as `Bearer <token>` for all endpoints.

### API Documentation

This service includes OpenAPI (Swagger) documentation for all endpoints.

- Swagger UI: [http://localhost:8083/swagger-ui/index.html](http://localhost:8083/swagger-ui/index.html)
- OpenAPI spec: [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs)

> Swagger UI is auto-configured via Springdoc and available when the service is running locally or via Docker.


### Future Improvements
- Add pagination and filtering for transaction history
- Centralized audit logging integration
- API gateway implementation

### License

This project is licensed under the MIT License.