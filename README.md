# Booking System with Race Condition Protection

## 1. Project Overview
This project is a backend Booking System developed as a Java/Spring Boot technical assignment. The primary problem it solves is concurrency: multiple users may attempt to book the same slot at the exact same time. The system guarantees that a slot cannot be double-booked under concurrent requests by utilizing database-level locking. 

## 2. Technology Stack
* Java 17
* Spring Boot (4.1.1)
* Spring Web (REST APIs)
* Spring Data JPA
* Spring Security (HTTP Basic Authentication)
* H2 Database (In-Memory)
* Maven
* JUnit 5 & MockMvc (Integration Testing)
* JaCoCo (Test Coverage)
* SonarQube (Code Quality configuration included)

## 3. Architecture
The application uses a standard Spring layered architecture:
`Client -> Controller -> Service -> Repository -> H2 Database`

* **Controller**: Handles HTTP/API routing, authentication principal extraction, and basic request validation.
* **Service**: Contains business logic, manages `@Transactional` boundaries, and enforces booking/cancellation rules.
* **Repository**: Interfaces with the database and requests pessimistic database locks.
* **Entity**: The database/domain models representing the system data.
* **Exception**: Centralized error handling translating domain exceptions to HTTP responses via `@RestControllerAdvice`.

## 4. Project Structure
The core application is located inside `backend/src/main/java/com/example/bookingsystem/` with the following structure:
```
controller/
service/
repository/
entity/
dto/
exception/
security/
```

## 5. Domain Model

### User
* `id`: Primary Key
* `username`: Unique username
* `password`: BCrypt encoded password
* `role`: Enum containing `USER` and `ADMIN`

### Slot
* `id`: Primary Key
* `startTime`: When the bookable slot begins
* `endTime`: When the bookable slot ends
* `status`: Enum (`AVAILABLE` or `BOOKED`)

### Booking
* `id`: Primary Key
* `slot`: Foreign Key to the booked Slot
* `user`: Foreign Key to the User who made the booking
* `status`: Enum (`ACTIVE` or `CANCELLED`)
* `createdAt`: Timestamp of booking creation

**Key Rule**: One Slot can have at most one `ACTIVE` booking. Historical `CANCELLED` bookings remain in the database as an audit log.

## 6. Authentication and Authorization
The project uses stateless HTTP Basic Authentication via Spring Security. The authenticated user is identified by the Spring Security Principal. The client does NOT provide a `userId` when creating a booking; instead, the server safely derives the booking owner from the authenticated identity.

**Roles and Permissions:**
* **USER**: Can view slots, book slots, and cancel their *own* bookings.
* **ADMIN**: Can view slots, create slots, and cancel *any* booking.

*Note: 401 Unauthorized is returned for unauthenticated requests, and 403 Forbidden is returned for authenticated users lacking the required role/ownership.*

*Development credentials are auto-generated via `DataInitializer` using Spring `Environment` properties with fallbacks. Do not use default credentials in production environments.*

## 7. API Documentation

### Create Slot
* **Method/URL**: `POST /slots`
* **Role**: `ADMIN`
* **Request Example**:
  ```json
  {
    "startTime": "2026-09-10T10:00:00",
    "endTime": "2026-09-10T11:00:00"
  }
  ```
* **Response**: 201 Created with slot details.

### Get Slots
* **Method/URL**: `GET /slots`
* **Role**: `USER` or `ADMIN`
* **Response**: 200 OK with list of all slots.

### Book Slot
* **Method/URL**: `POST /bookings`
* **Role**: `USER`
* **Request Example**:
  ```json
  {
    "slotId": 1
  }
  ```
  *(Important: Do NOT send userId. The authenticated user's identity is taken securely from Spring Security.)*
* **Response**: 201 Created (success) or 409 Conflict (if slot is already booked).

### Cancel Own Booking
* **Method/URL**: `POST /bookings/{id}/cancel`
* **Role**: `USER`
* **Rules**: A user can only cancel their own `ACTIVE` booking. 
* **Response**: 200 OK (success) or 403 Forbidden (if trying to cancel another user's booking).

### Admin Cancel Booking
* **Method/URL**: `POST /admin/bookings/{id}/cancel`
* **Role**: `ADMIN`
* **Rules**: Admin can cancel any `ACTIVE` booking.
* **Response**: 200 OK.

## 8. Booking Flow
1. `POST /bookings` is called.
2. Spring Security authenticates the user.
3. Controller obtains the authenticated username and delegates to `BookingService.createBooking()`.
4. `@Transactional` transaction starts.
5. `PESSIMISTIC_WRITE` lock is acquired on the Slot row.
6. The Slot status is evaluated.
   * If `AVAILABLE`: Creates the Booking, sets Slot to `BOOKED`, commits the transaction, and returns 201 Created.
   * If `BOOKED`: Throws a `SlotAlreadyBookedException`, which is caught by the `GlobalExceptionHandler` returning a 409 CONFLICT.

## 9. Race Condition Protection
Preventing double bookings under high concurrency is a core focus of this system.

If User A and User B simultaneously try to book Slot 1, both transactions attempt a `SELECT ... FOR UPDATE` on the database row. The database-level pessimistic write lock ensures only one transaction can hold the write lock for the Slot row at a time.

**Execution Flow:**
* **Transaction A**: Obtains Slot 1 lock, sees `AVAILABLE`, creates the booking, changes Slot to `BOOKED`, and commits.
* **Transaction B**: Waits for the Slot lock at the database level. Once Transaction A commits and releases the lock, Transaction B obtains it, reads the *updated* Slot state, sees `BOOKED`, and throws a 409 Conflict.

If 10 concurrent requests target the same slot simultaneously, the result is exactly 1 successful booking and 9 rejected requests (yielding exactly 1 `ACTIVE` booking). 

This application deliberately avoids in-memory locks (like `synchronized`, `ReentrantLock`, or `ConcurrentHashMap`) to ensure standard, robust database-coordinated synchronization.

## 10. Important H2 Configuration Note
The assignment uses an in-memory H2 database (`jdbc:h2:mem:bookingdb`). This means the database and data only exist while the application is running and are wiped upon restart.

*Note on limitations*: An in-memory H2 database is local to the JVM instance. If two separate application instances were running concurrently with separate in-memory databases, they would not share the same Slot rows or locks. This is a limitation of the assignment's H2 configuration requirement, not the pessimistic locking strategy. For a production multi-instance deployment, the application would connect to a shared persistent database (like PostgreSQL/MySQL), where database-level pessimistic locking natively coordinates all application instances.

## 11. Cancellation Flow
Booking cancellations also securely coordinate through the Slot database lock to prevent race conditions (e.g., someone trying to book a slot while it is simultaneously being cancelled).

**USER cancellation:**
* Authenticate user -> Find booking -> Verify ownership -> Lock associated Slot -> Verify `ACTIVE` state -> Set Booking to `CANCELLED` -> Set Slot to `AVAILABLE` -> Commit.

**ADMIN cancellation:**
* Authenticate admin -> Find booking -> Lock associated Slot -> Verify `ACTIVE` state -> Set Booking to `CANCELLED` -> Set Slot to `AVAILABLE` -> Commit.

## 12. Transaction Management
Transaction boundaries are tightly placed at the service layer using `@Transactional`.
* `@Transactional createBooking(...)`
* `@Transactional cancelBooking(...)`
* `@Transactional cancelBookingAsAdmin(...)`

This ensures that `Slot` and `Booking` table updates are completely atomic. A successful or failed operation will never leave the database in an inconsistent state (e.g., leaving a Slot `AVAILABLE` with an `ACTIVE` booking, or leaving a Slot `BOOKED` without an `ACTIVE` booking).

## 13. Exception Handling
The project uses a `@RestControllerAdvice` class (`GlobalExceptionHandler`) to translate domain exceptions into standardized JSON error responses.

* **404 Not Found**: Target Slot or Booking does not exist.
* **409 Conflict**: Slot is already booked, or attempting an invalid state transition (e.g., cancelling an already cancelled booking).
* **403 Forbidden**: User attempting to interact with another user's resources.
* **400 Bad Request**: Invalid JSON payloads or failed validation.

## 14. Testing
The project contains comprehensive integration and real-world concurrency tests using `CountDownLatch`.

**Current verified status**:
* **39 tests** (0 failures, 0 errors).
* Concurrency Test Example: 10 concurrent booking threads attempt to book the same slot simultaneously. The test asserts that exactly 1 booking is created and 9 threads receive 409 Conflict exceptions. A similar test guarantees safety between simultaneous booking and cancellation.

## 15. Test and Coverage Commands
To run the tests locally:
```bash
# Windows
cd backend
./mvnw.cmd clean test
```
To run the tests and enforce the JaCoCo 80% line coverage threshold:
```bash
./mvnw.cmd clean verify
```
The JaCoCo coverage report will be generated at `target/site/jacoco/index.html`. 
*(Note: Current verified line coverage is approximately 84%).*

## 16. Running the Application
To run the application locally on Windows:
```bash
cd backend
./mvnw.cmd spring-boot:run
```
The REST API will be available at: `http://localhost:8080`

## 17. H2 Console
The H2 database console is enabled for local development/debugging and can be accessed at:
`http://localhost:8080/h2-console`

* **JDBC URL**: `jdbc:h2:mem:bookingdb`
* **Username**: `sa`
* **Password**: *(leave empty)*

Useful Queries:
```sql
SELECT * FROM USERS;
SELECT * FROM SLOTS;
SELECT * FROM BOOKINGS;
SELECT * FROM BOOKINGS WHERE STATUS = 'ACTIVE';
```
*(Remember that the H2 database resets when the application restarts).*

## 18. Validation and Error Responses
The API guarantees standardized error responses. Example payload for a double-booking attempt:
```json
{
  "timestamp": "2026-09-06T12:00:00.000+00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Slot is already booked",
  "path": "/bookings"
}
```

## 19. SonarQube
The project includes SonarQube Maven plugin configuration and integrates the JaCoCo XML report. Since no active SonarQube/SonarCloud server was provided in the development environment, server-side quality gate analysis was not executed, but the project is configured for it.

To execute the analysis in a CI/CD pipeline:
```bash
./mvnw.cmd clean verify sonar:sonar \
  -Dsonar.host.url=$SONAR_HOST_URL \
  -Dsonar.login=$SONAR_TOKEN \
  -Dsonar.projectKey=bookingsystem
```

## 20. Assumptions / Design Decisions
1. **HTTP Basic Authentication**: Selected for simple, stateless API authentication suitable for the bounds of this assignment.
2. **Server-Side Identity**: User identity is extracted from Spring Security rather than trusting client-supplied payloads (`userId`).
3. **Pessimistic DB Locking**: Selected because booking requires strict serialized access to the Slot row.
4. **H2 In-Memory DB**: Used to satisfy the assignment requirements for portability.
5. **No JVM/Application Locking**: `synchronized` and `ReentrantLock` are avoided in favor of distributed-safe database locking.
6. **Audit Records**: Cancelled bookings remain in the database as historical records.
7. **Cardinality**: Only one `ACTIVE` booking is allowed per `Slot`.

## 21. Limitations
As noted in Section 10, the requirement to use an H2 in-memory database means that pessimistic locking will not synchronize requests spanning across multiple independent, separately deployed instances of this application (since they do not share the same database).

## 22. Future Improvements
For a production rollout, the following architectural upgrades would be recommended:
* Replace H2 with a shared persistent database (PostgreSQL/MySQL) to support multi-instance deployment.
* Replace HTTP Basic Auth with stateless JWT or OAuth2 depending on client requirements.
* Extract configuration/credentials to an external vault or Config Server.
* Introduce Swagger/OpenAPI documentation.
* Implement monitoring and observability (Micrometer/Prometheus).

## 23. Submission
This repository contains the complete assignment implementation. The backend successfully meets all requirements, safely handles race conditions using standard relational database mechanisms, achieves >80% test coverage through robust integration tests, and strictly enforces authorization rules.
