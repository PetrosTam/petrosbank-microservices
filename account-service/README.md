# Account Service
The Account Service is responsible for managing user bank accounts and balances within the Banking App ecosystem. It supports secure account creation, retrieval, updates, and administrative actions, and integrates with other services like the Transaction Service.

### Features
- Create new bank accounts for registered and authenticated users
- Secure access to account details using JWT authentication
- Retrieve all accounts for the logged-in user, view specific account details, and check current balance and status.
- Track and update account balances via internal service calls
- Freeze and delete accounts (admin-only operation)
- Role-aware endpoint design for users, admins, and internal services
- Clean separation of Account and AccountBalance entities
- OpenAPI documentation via Swagger UI
- Dockerized for easy local and multi service orchestration


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
   git clone https://github.com/your-username/account-service.git
   cd account-service

2. **Configure application properties**

    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/accountdb
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
  docker-compose up --build account-service
```
- Exposes port 8082
- Registers with Eureka at startup
- Required environment variables:
    - JWT_SECRET
    - EUREKA_SERVER_URL
    - DB_URL, DB_USERNAME, DB_PASSWORD
  
### API Endpoints

### User operations

| Method | Endpoint                                   | Description                          |
|--------|--------------------------------------------|--------------------------------------|
| POST   | `/api/v1/accounts`                         | Create a new account                 |
| GET    | `/api/v1/accounts/me/{accountNumber}`      | Fetch current user's account details |
| GET    | `/api/v1/accounts/me`                      | Fetch all accounts for current user  |
| GET    | `/api/v1/accounts/{accountNumber}/status`  | Fetch account balance                |

### Admin operations

| Method | Endpoint                                   | Description    |
|--------|--------------------------------------------|----------------|
| DELETE | `/api/v1/accounts/{accountNumber}`         | Delete account |
| PUT    | `/api/v1/accounts/{accountNumber}/status`  | Freeze account |

### Internal Service Operations (Transaction Service)

| Method | Endpoint                            | Description             |
|--------|-------------------------------------|-------------------------|
| GET    | `/api/v1/accounts/{accountNumber}`  | Fetch account details   |
| PUT    | `/api/v1/accounts/{accountNumber}`  | Update account balance  |


> JWT must be included in the `Authorization` header as `Bearer <token>` for all endpoints.

### API Documentation

This service includes OpenAPI (Swagger) documentation for all endpoints.

- Swagger UI: [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)
- OpenAPI spec: [http://localhost:8082/v3/api-docs](http://localhost:8082/v3/api-docs)

> Swagger UI is auto-configured via Springdoc and available when the service is running locally or via Docker.


### Future Improvements
- Add pagination for account listing 
- Centralized audit logging integration
- API gateway implementation

### License

This project is licensed under the MIT License.