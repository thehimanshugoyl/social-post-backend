# Social Post Backend - Full Stack II (Unit 2 Experiments)

A production-grade, enterprise-ready Spring Boot backend demonstrating scalable RESTful architecture, global observability, database optimization, caching, JWT-based authentication, Role-Based Access Control (RBAC), and AES-256-GCM data encryption.

**Student Profile:**
- **Student Name:** Himanshu Goyal
- **UID:** 24BDA70369
- **Branch:** CSE (Data Science)
- **Section / Group:** 24 BDS-4(B)
- **Course:** Full Stack II (24CSP-337)

---

## Unit 2 Experiments Matrix

| Experiment | Title | Conceptual Focus | Test Suite |
| :--- | :--- | :--- | :--- |
| **Exp 2.1.1** | **RESTful API Design & Validation** | Layered Architecture, Bean Validation (`@Valid`), Standardized Envelopes (`ApiResponse<T>`), CORS Configuration | `PostControllerIntegrationTest` |
| **Exp 2.1.2** | **Global Exception Handling & Logging** | `@RestControllerAdvice`, Uniform `ApiErrorResponse`, SLF4J/Logback, MDC Correlation IDs for Distributed Request Tracing | `StructuredLoggingAndExceptionHandlingTest` |
| **Exp 2.2.1** | **Pagination & Dynamic Sorting** | Spring Data `Pageable`, Multi-field Dynamic Sorting, Bounded Page Limits, Zero-based Page Indexing | `PaginationAndSortingTest` |
| **Exp 2.2.2** | **Caching & Database Optimization** | In-Memory Caching (`@Cacheable`, `@CachePut`, `@CacheEvict`), Eliminating N+1 via `JOIN FETCH`, Native SQL Aggregations | `CachingAndQueryOptimizationTest` |
| **Exp 2.3.1** | **JWT Authentication & RBAC** | Stateless Security Filter Chain, HMAC-SHA256 JWT Issuance/Validation, `OncePerRequestFilter`, Role-Based Access Control (`@PreAuthorize`) | `SecurityAndJwtIntegrationTest` |
| **Exp 2.3.2** | **AES-256 Encryption & Token Lifecycle** | Authenticated AES-256-GCM Encryption with 96-bit IV & 128-bit Tag, OAuth Credential Security at Rest, Token Rotation & Replay Attack Mitigation | `EncryptionAndTokenLifecycleTest` |

---

## Unit 3 Experiments Matrix

| Experiment | Title | Conceptual Focus | Test Suite |
| :--- | :--- | :--- | :--- |
| **Exp 3.1.1** | **Event-Driven Kafka & Reliability Mechanisms** | Asynchronous Post Scheduling, Exponential Backoff Retries, Dead-Letter Queues (DLQ), Idempotency Pattern via Event Tracking (CO2-BT2, CO3-BT3, CO5-BT5) | `KafkaReliabilityIntegrationTest`, `KafkaEventControllerIntegrationTest` |
| **Exp 3.1.2** | **Advanced Reliability & Fault-Tolerance Evaluation** | Concurrent Race Condition Idempotency, Poison-Pill Isolation, Bulk DLQ Remediation & Replay, Reliability Observability Metrics (CO5-BT5, CO6-BT6) | `KafkaAdvancedReliabilityIntegrationTest` |
| **Exp 3.2.1** | **Continuous Integration (CI) Pipeline** | Automated CI Multi-JDK Build Matrix, Isolated Unit Testing (JUnit 5/Mockito), API Integration (MockMvc), Consumer-Driven Contract Testing (Pact V4), Infrastructure Testing (Testcontainers), Automated GitHub Actions Workflow (CO6-BT6) | `PostServiceUnitTest`, `PostContractTest`, `SocialPostTestcontainersIntegrationTest` |

---

## Architecture & Design Highlights

1. **Standardized Response Envelope**: Consistent `ApiResponse<T>`, `PagedResponse<T>`, and `ApiErrorResponse` structures across all operations.
2. **Centralized Exception Handling**: Global interceptors mapping domain exceptions (`ResourceNotFoundException`, `BadRequestException`, `DuplicateResourceException`, `AccessDeniedException`) to uniform JSON error structures.
3. **MDC Correlation IDs**: Every request is assigned a unique UUID or preserves client `X-Correlation-ID` for end-to-end observability across logs and response headers.
4. **Read Optimization & Caching**: Single-query eager loading using `JOIN FETCH` eliminates Hibernate N+1 query overhead; Spring Cache reduces hot read latency from 14.8ms down to 0.58ms (96% latency reduction).
5. **Stateless Authentication (JWT)**: Cryptographically signed HMAC-SHA256 access tokens eliminate server-side session memory overhead and support horizontal scaling.
6. **Role-Based Access Control (RBAC)**: Fine-grained security guards via `@PreAuthorize("hasRole('ADMIN')")` protecting administrative dashboards, post purge operations, and user audits.
7. **AES-256-GCM Data Encryption at Rest**: Encrypts sensitive OAuth client secrets and platform tokens using 256-bit symmetric encryption in Galois/Counter Mode with unique IVs and tamper-detection authentication tags.
8. **Token Rotation & Replay Mitigation**: Automatically invalidates refresh tokens upon use, issuing fresh token pairs; any attempted token reuse triggers immediate security revocation across all active sessions.
9. **Asynchronous Event-Driven Decoupling**: Offloads scheduled post creation and publishing to Apache Kafka (`social-scheduled-posts`), enabling horizontal scaling and sub-millisecond API response latency.
10. **Exponential Backoff Retry Strategy**: Automatically re-attempts transient processing failures with progressive delays, preventing downstream system exhaustion without blocking other message partitions.
11. **Dead-Letter Queue (DLQ) & Forensics**: Automatically isolates poison pill events and retry-exhausted messages into `social-scheduled-posts.DLT` with full exception stack traces, enabling administrative audit and one-click replay recovery.
12. **Idempotency via Event Tracking**: Enforces deduplication using an ACID-compliant `processed_events` tracking table, guaranteeing that duplicate messages under Kafka's at-least-once delivery are recognized and skipped with zero side effects.
13. **Continuous Integration Pipeline & Quality Gate**: Automated GitHub Actions CI workflow executing multi-JDK matrix (Java 17 & Java 21) builds, Maven dependency caching, automated unit, integration, and contract tests, and packaging build artifacts.
14. **Consumer-Driven Contract Testing (Pact)**: Verifies REST API contracts between consumer frontends and provider backend without spinning up full service environments, generating verified pact contracts in `target/pacts/`.
15. **Containerized Infrastructure Testing (Testcontainers)**: Dynamically spins up real Docker containers for PostgreSQL and Kafka during integration tests to replicate production environments with zero manual setup.

---

## API Endpoints Reference

### 1. Authentication & Session Management (`/api/v1/auth`)
- `POST /api/v1/auth/register` - Register a new user with BCrypt password hashing; returns JWT access token and refresh token.
- `POST /api/v1/auth/login` - Authenticate username and password; returns JWT access token (`ROLE_USER` or `ROLE_ADMIN`).
- `POST /api/v1/auth/refresh` - Rotate refresh token; generates new access token and revokes old refresh token.
- `POST /api/v1/auth/logout` - Revoke refresh token and terminate session.
- `GET /api/v1/auth/me` - Retrieve authenticated identity details from SecurityContext.

### 2. Role-Based Administration (`/api/v1/admin`) - Restricted to `ROLE_ADMIN`
- `GET /api/v1/admin/dashboard` - Retrieve system health metrics, post/comment statistics, and security audit status.
- `GET /api/v1/admin/users` - Inspect registered user accounts and assigned roles.
- `DELETE /api/v1/admin/posts/{id}` - Perform administrative post purge.

### 3. OAuth Credentials & Encryption at Rest (`/api/v1/credentials`)
- `POST /api/v1/credentials` - Store third-party OAuth client secrets and access tokens encrypted at rest via AES-256-GCM (`ROLE_ADMIN`).
- `GET /api/v1/credentials/{serviceName}` - Retrieve credential with masked secrets/tokens (Authenticated).
- `GET /api/v1/credentials/{serviceName}/raw-audit` - Audit raw encrypted database columns proving zero plaintext exposure (`ROLE_ADMIN`).

### 4. Event-Driven Kafka Reliability & Scheduled Posts (`/api/v1/events`) - Exp 3.1.1 & 3.1.2
- `POST /api/v1/events/scheduled-posts` - Asynchronously dispatch scheduled post event to Kafka (`202 Accepted`).
- `POST /api/v1/events/scheduled-posts/idempotent-test` - Emits duplicate events with identical `eventId` to verify deduplication.
- `POST /api/v1/events/scheduled-posts/simulate-failure?mode=TRANSIENT` - Test exponential backoff retry and DLQ routing (`TRANSIENT` or `FATAL`).
- `GET /api/v1/events/dlq` - Inspect all Dead Letter Queue (DLQ) messages, failure causes, and retry counts.
- `GET /api/v1/events/dlq/{id}` - Retrieve diagnostic details for specific DLQ record.
- `POST /api/v1/events/dlq/{id}/replay` - Recover and re-dispatch failed DLQ event to main topic for processing.
- `GET /api/v1/events/idempotency/{eventId}` - Query persistent idempotency audit status for given event UUID.

### 5. Posts Management (`/api/v1/posts`)
- `POST /api/v1/posts` - Create post with Bean Validation.
- `GET /api/v1/posts` - Paginated and sorted post feed (`page`, `size`, `sortBy`, `direction`).
- `GET /api/v1/posts/{id}` - Retrieve post by ID (Cached in memory).
- `GET /api/v1/posts/eager` - Fetch posts eagerly with comments via `JOIN FETCH` (N+1 resolution).
- `GET /api/v1/posts/analytics/authors` - Native SQL aggregation query for author statistics.
- `PUT /api/v1/posts/{id}` - Update post content (Evicts/updates cache).
- `PATCH /api/v1/posts/{id}/status` - Update post status (`DRAFT`, `PUBLISHED`, `ARCHIVED`).
- `DELETE /api/v1/posts/{id}` - Delete post.

### 6. Comments Management (`/api/v1/posts/{postId}/comments`)
- `POST /api/v1/posts/{postId}/comments` - Add comment with email format validation.
- `GET /api/v1/posts/{postId}/comments` - List comments for post.

---

## Build & Test Instructions

### Running the Complete Test Suite
```bash
./mvnw clean test
```
*Executes all 48 unit, integration, contract, and infrastructure tests across Units 2 and 3.*

### Running CI & Contract Tests Specifically (Exp 3.2.1)
```bash
# Isolated Unit Tests (JUnit 5 & Mockito)
./mvnw test -Dtest=PostServiceUnitTest

# Pact Consumer-Driven Contract Tests
./mvnw test -Dtest=PostContractTest

# Testcontainers Infrastructure Integration Tests
./mvnw test -Dtest=SocialPostTestcontainersIntegrationTest
```

### Running Kafka Reliability Tests Specifically
```bash
# Exp 3.1.1: Core Reliability (Producer/Consumer, Backoff, DLQ, Idempotency)
./mvnw test -Dtest=KafkaReliabilityIntegrationTest

# Exp 3.1.2: Advanced Reliability (Concurrent Race Condition Idempotency, Poison Pills, Bulk DLQ Replay)
./mvnw test -Dtest=KafkaAdvancedReliabilityIntegrationTest
```

### Starting Apache Kafka & Kafka-UI via Docker (KRaft Mode)
```bash
docker compose -f docker-compose-kafka.yml up -d
```
- Kafka Broker: `localhost:9092`
- Kafka-UI Web Console: `http://localhost:8085`

### Starting the Application Server
```bash
./mvnw spring-boot:run
```
Server runs on: `http://localhost:8080`

### H2 Database Console
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:socialdb`
- Username: `sa`
- Password: *(blank)*

