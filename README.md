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

This runs **unit and controller tests** using `MockMvc`, `Mockito`, and Spring Boot's `@WebMvcTest`.

### ❗ Test Limitations (Time Constraint)

Due to time constraints, the following areas were **not covered**:

- **Integration tests** with real infrastructure (e.g., using TestContainers)
- **Code coverage reports** (e.g., via JaCoCo or PIT)
- **End-to-end tests** for full async SQS flows

To partially mitigate this, a [Postman Collection](./support/postman_collection/wallet-recargapay.postman_collection.json) is included, which can be used to **manually verify**:

- Wallet creation
- Transfers
- Withdrawals
- Deposit simulations
- Validation errors and boundary cases

## System Architecture

![img.png](img.png)

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
- **Integration tests**: Due to time constraints, integration tests were not implemented using TestContainers. Instead, a [Postman Collection](./support/postman_collection/wallet-recargapay.postman_collection.json) was included to manually run and verify the main workflows.

## Project Structure

```
com.recargapay.wallet
├── config               # All @Configuration classes
├── exception            # Error Handlers and custom Exceptions
├── helper               # JSON helper for serialize and deserialize JSON
├── controller           # REST endpoints
├── usecase              # Business logic
├── persistence.entity   # JPA entities
├── persistence.service  # DB access logic
├── mapper               # DTO mapping
├── integration          # Adapters for Redis, SQS, and external APIs
    └── http.corebanking # Core Banking Integration
```

## Core Banking Integration

The solution is designed around the concept of a **dedicated core banking subsystem**, responsible for handling all operations that interact with banking infrastructure. This subsystem encapsulates:

- **CVU/Alias provisioning** for wallet creation
- **Deposits and withdrawals**, including validations and coordination with external financial entities (e.g., BCRA)
- **Account management**, ensuring alias/CVU/CBU uniqueness and verifying whether accounts are internal or external
- **Information lookups** by alias or CVU/CBU
- **Asynchronous communication** using **Amazon SQS**, emitting relevant events (e.g., deposit confirmed, withdrawal completed)

## Utility Endpoints

The `UtilsController` provides auxiliary endpoints that are **not part of the main wallet flow**, but serve specific purposes for testing, observability, and support during development.

### Features

- **Simulated Deposits**  
  The `/utils/simulate/deposit` endpoint allows manual simulation of incoming deposits for development or test environments. In production, these deposits would typically be delivered asynchronously through the `deposit-arrived` SQS queue from the CoreBanking subsystem.

  **Important:** Each simulated deposit must use a **unique `external_tx_id`**. If the same `external_tx_id` is used more than once, the deposit will be **ignored** as a duplicate event.

- **Account Listing by Alias**  
  The `/utils/account-by-alias` endpoint retrieves all provisioned accounts (CVU/CBU), with optional filtering by Recargapay-managed (`isRpUser`) accounts. Useful for debugging and observing CVU/alias provisioning during wallet creation.

**Example Payload for Simulated Deposit:**

```json
{
  "amount": 500000,
  "destination_alias": "test.1ars.rp",
  "source_cbu": "2850590940090418135201",
  "source_cvu": null,
  "external_tx_id": "00000-00001"
}
```

## Database Connection

The service connects to a local MySQL instance configured as follows:

- **Host**: `localhost`
- **Port**: `3306`
- **Database name**: `recargapay`
- **Username**: `root`
- **Password**: `verysecret`

### JDBC Connection URL

```
jdbc:mysql://localhost:3306/recargapay?allowPublicKeyRetrieval=true&useSSL=false
```

The schema is initialized automatically using scripts in the `support/docker/config/script` directory when using Docker Compose.

## Author

Leo Morganti – [linkedin.com/in/leomorganti](https://www.linkedin.com/in/leonardo-morganti-47045b103/)