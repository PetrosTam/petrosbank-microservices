# Users Service
Handles user registration, authentication, and profile management for the Banking App ecosystem. Built with Spring Boot and designed for secure, scalable integration within a microservices architecture.


### Features
- User registration with validation
- Secure login with JWT issuance
- Role-based access control
- Profile retrieval and updates
- Audit logging for key actions

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
   git clone https://github.com/your-username/user-service.git
   cd user-service

2. **Configure application properties**

    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5432/usersdb
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
  docker-compose up --build user-service
```
- Exposes port 8081
- Registers with Eureka at startup
- Required environment variables:
    - JWT_SECRET
    - EUREKA_SERVER_URL
    - DB_URL, DB_USERNAME, DB_PASSWORD

### API Endpoints

| Method | Endpoint                 | Description                   |
|--------|--------------------------|-------------------------------|
| POST   | `/api/v1/users/register` | Register new user             |
| POST   | `/api/v1/users/login`    | Authenticate user & get token |
| GET    | `/api/v1/users/{id}`     | Get current user profile      |
| PUT    | `/api/v1/users/{id}`     | Update current user profile   |

> JWT must be included in the `Authorization` header as `Bearer <token>` for protected endpoints.

### API Documentation

This service includes OpenAPI (Swagger) documentation for all endpoints.

- Swagger UI: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- OpenAPI spec: [http://localhost:8081/v3/api-docs](http://localhost:8081/v3/api-docs)

> Swagger UI is auto-configured via Springdoc and available when the service is running locally or via Docker.


### Security
This service uses Spring Security with:
- BCrypt password hashing
- JWT token authentication (stateless)
- Role-based access control (ROLE_USER, ROLE_ADMIN, etc.)
- Global exception handling for authentication/authorization errors

### Example JWT Authentication Flow
- User logs in and receives JWT token
- All subsequent requests include Authorization: Bearer <token>
- Token is verified by a JwtAuthenticationFilter

### Future Improvements
- Email verification on registration
- Password reset functionality
- OAuth2 support (Google, Facebook, etc.)
- Rate limiting
- API gateway implementation

### License

This project is licensed under the MIT License.