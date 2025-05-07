# Recargapay Wallet Microservice

This repository contains the implementation of a wallet microservice that supports:

- Wallet creation with CVU/alias provisioning
- Fund transfers between wallets
- Withdrawals to external bank accounts
- Retrieval of historical balance at a specific point in time

## Tech Stack

- Java 21
- Spring Boot 3.4.5
- Maven
- Redis (mutex & caching)
- MySQL 8.0.34
- LocalStack (SQS simulation)
- Docker Compose
- Swagger / OpenAPI

## Running the Service Locally

### Prerequisites

- Java **21** must be installed and active (e.g. `openjdk version "21.0.x"`)

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/recargapay_wallet.git
cd recargapay_wallet
```
### 2. Build the Docker image

```bash
./mvnw clean spring-boot:build-image -Dspring-boot.build-image.imageName=recargapay/wallet-service
```
### 3. Start the environment using Docker

```bash
cd support/docker/
./app-down-delete-vol.sh 
./app-up-create-all.sh
```

### 4. Wait for the application logs

Ensure both `wallet-service-1-1` and `wallet-service-2-1` show:

```
Started RecargapayWalletApplication
```

### 5. Access Swagger UI

- http://localhost:8080/swagger-ui.html
- http://localhost:8081/swagger-ui.html

## Running Tests

```bash
./mvnw clean test
```

## Design Decisions

- **Hexagonal architecture**: Use cases are isolated from infrastructure for testability and scalability.
- **Redis-based mutex**: Prevents duplicate wallet creation and race conditions in balance updates.
- **SQS simulation**: LocalStack is used to emulate asynchronous events like deposits, withdrawals, and CVU provisioning.
- **Validation strategy**: DTOs and use cases leverage Spring’s validation annotations.
- **API documentation**: Swagger/OpenAPI integrated via springdoc.

## Trade-offs and Time Constraints

- **Mocked integrations**: CoreBanking and Notification systems were stubbed for simplicity.
- **Simplified error handling**: Responses use `ProblemDetail`, but without localization or error codes.
- **No authentication**: Auth concerns were omitted to focus on functional requirements.

## Project Structure

```
com.recargapay.wallet
├── config               # All @Configuration classes
├── exception            # Error Hanlers and custom Exceptions
├── helper               # JSON helper for serialize and deserialize JSON
├── controller           # REST endpoints
├── usecase              # Business logic
├── persistence.entity   # JPA entities
├── persistence.service  # DB access logic
├── integration          # Adapters for Redis, SQS, and external APIs
├── mapper               # DTO mapping
```
## Core Banking Integration

The solution is designed around the concept of a **dedicated core banking subsystem**, responsible for handling all operations that interact with banking infrastructure. This subsystem encapsulates:

- **CVU/Alias provisioning** for wallet creation
- **Deposits and withdrawals**, including validations and coordination with external financial entities (e.g., BCRA)
- **Account management**, ensuring alias/CVU/CBU uniqueness and verifying whether accounts are internal or external
- **Information lookups** by alias or CVU/CBU
- **Asynchronous communication** using **Amazon SQS**, emitting relevant events (e.g., deposit confirmed, withdrawal completed)

This design isolates banking responsibilities from the wallet business logic, enabling:
- Better scalability and modularity
- Easy mocking and simulation in local environments (e.g., via `MockCoreBankingClientImpl`)
- Decoupled integration with real-world banking APIs

All communication with this subsystem happens through a clean interface (`CoreBankingClient`) with implementations that can vary between real and mock clients, depending on the runtime profile or testing scenario.

## Author

Leo Morganti – [linkedin.com/in/leomorganti](https://www.linkedin.com/in/leonardo-morganti-47045b103/)
