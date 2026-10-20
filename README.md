# BookNow — High-Concurrency Booking Platform

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-14%2B-blue.svg)](https://www.postgresql.org/)
[![React](https://img.shields.io/badge/React-19.2-blue.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8.2-purple.svg)](https://vitejs.dev/)
[![JaCoCo](https://img.shields.io/badge/Coverage->=80%25-green.svg)](https://www.jacoco.org/)

**BookNow** is a production-oriented, full-stack appointment and slot reservation platform engineered to solve one of the most critical challenges in distributed systems: **race-condition protection and double-booking prevention**.

When multiple users attempt to claim the exact same bookable slot at the identical millisecond, traditional non-locking architectures permit multiple simultaneous confirmations—causing data corruption, customer disputes, and business failures. BookNow eliminates this concurrency hazard by leveraging **database-level row locking (`SELECT ... FOR UPDATE` / `FOR NO KEY UPDATE`)**, atomic transaction boundaries, and state evaluation on **PostgreSQL**.

---

## Table of Contents
- [1. Project Overview](#1-project-overview)
- [2. Screenshots & UI Mockups](#2-screenshots--ui-mockups)
- [3. Technology Stack](#3-technology-stack)
- [4. Architecture & Project Structure](#4-architecture--project-structure)
- [5. Features & Business Rules](#5-features--business-rules)
- [6. Concurrency & Race-Condition Deep Dive](#6-concurrency--race-condition-deep-dive)
- [7. Prerequisites & Local Setup](#7-prerequisites--local-setup)
- [8. Environment Configuration](#8-environment-configuration)
- [9. Demo Login Credentials](#9-demo-login-credentials)
- [10. Complete Application Usage Guide](#10-complete-application-usage-guide)
  - [Scenario A: Log in as Admin and Create a Slot](#scenario-a-log-in-as-admin-and-create-a-slot)
  - [Scenario B: Log in as User 1 on Device 1](#scenario-b-log-in-as-user-1-on-device-1)
  - [Scenario C: Log in as User 2 on Device 2](#scenario-c-log-in-as-user-2-on-device-2)
  - [Scenario D: Test Concurrent Booking Requests](#scenario-d-test-concurrent-booking-requests)
  - [Scenario E: Verify Final Database State](#scenario-e-verify-final-database-state)
- [11. API Documentation](#11-api-documentation)
- [12. Error Handling & Standardized Responses](#12-error-handling--standardized-responses)
- [13. Testing, Coverage & Quality](#13-testing-coverage--quality)
- [14. Production Deployment Guide](#14-production-deployment-guide)
- [15. Troubleshooting Guide](#15-troubleshooting-guide)
- [16. Security & Limitations](#16-security--limitations)
- [17. Contribution & License](#17-contribution--license)

---

## 1. Project Overview

### Purpose & Problem Statement
In reservation systems (e.g., medical clinics, co-working spaces, interview schedulers, salon appointments), slots are a strictly finite resource. If User 1 and User 2 both click "Confirm Booking" simultaneously:
- A naive read-then-write system checks slot availability for both users before either transaction commits.
- Both transactions observe the slot as `AVAILABLE`.
- Both transactions write a confirmation record, creating a **double-booking bug**.

BookNow prevents this problem at the database engine level. Using PostgreSQL row-level pessimistic locking, all incoming requests targeting the same slot row are serialized in the database. The winning transaction updates the slot to `BOOKED` and creates the booking, while all trailing requests observe the modified state and are immediately rejected with HTTP `409 Conflict`.

### Key Features
- **Strict Concurrency Protection**: Database row-level locks prevent double-booking under high load.
- **Role-Based Access Control (RBAC)**: `ADMIN` and `USER` roles enforced through Spring Security.
- **Admin Slot Management**: Admins configure bookable time slots with start/end timestamps.
- **Self-Service Booking & Cancellation**: Users book available slots and cancel their own active reservations.
- **Admin Override Cancellation**: Administrators can cancel any active booking.
- **Stateless HTTP Basic Authentication**: Robust, testable authentication using BCrypt password encoding.
- **Automated Data Seeding**: `DataInitializer` bootstraps demo accounts on startup.
- **Dual Environment Profiles**: Seamless switching between `local` (PostgreSQL) and `prod` (Render).
- **Automated Render URL Normalization**: Custom `EnvironmentPostProcessor` automatically translates Render connection strings (`postgresql://...`) to valid JDBC URLs.
- **Automated Integration & Concurrency Test Suite**: 48 automated tests with `CountDownLatch` multi-threaded race simulation and enforced **80%+ JaCoCo line coverage**.

---

## 2. Screenshots & UI Mockups

| Admin Dashboard (Slot Creation & Management) | User Booking Page (Available Slots) |
| :---: | :---: |
| ![Admin Dashboard Placeholder](https://via.placeholder.com/600x350/1e293b/ffffff?text=BookNow+Admin+Dashboard+-+Slot+Creation) | ![User Booking Page Placeholder](https://via.placeholder.com/600x350/0f172a/38bdf8?text=BookNow+User+Portal+-+Live+Slots) |

| Successful Booking Confirmation | Concurrency Conflict (HTTP 409) |
| :---: | :---: |
| ![Booking Success Placeholder](https://via.placeholder.com/600x350/064e3b/ffffff?text=Booking+Confirmed+-+Slot+Reserved) | ![Concurrency Conflict Placeholder](https://via.placeholder.com/600x350/7f1d1d/ffffff?text=409+Conflict+-+Slot+Already+Booked) |

---

## 3. Technology Stack

### Backend
- **Language**: Java 17 (Eclipse Temurin LTS)
- **Framework**: Spring Boot 4.1.1
- **Web**: Spring Web MVC (REST APIs)
- **Persistence**: Spring Data JPA / Hibernate ORM 7.4.5
- **Database Driver**: PostgreSQL JDBC Driver (`org.postgresql:postgresql`)
- **Security**: Spring Security (Stateless HTTP Basic, BCryptPasswordEncoder)
- **Build Tool**: Apache Maven with Maven Wrapper (`mvnw` / `mvnw.cmd`)
- **Testing**: JUnit 5, Spring Boot Test, Spring Security Test, MockMvc
- **Code Coverage**: JaCoCo Maven Plugin 0.8.12 (Enforced >= 80% line coverage gate)
- **Code Quality**: SonarQube Maven configuration

### Database
- **Engine**: PostgreSQL 14+ (Local verification performed on PostgreSQL 16.9)
- **Schema Management**: Hibernate DDL Auto (`update`) & SQL Migration Scripts (`V1__init_schema.sql`)
- **Isolation Level**: `READ_COMMITTED` with Row-Level Locking (`FOR NO KEY UPDATE` / `FOR UPDATE`)

### Frontend
- **Library**: React 19 (`^19.2.8`)
- **Tooling / Bundler**: Vite 8 (`^8.2.2`)
- **Linting**: Oxlint (`^1.79.0`)
- **Styling**: Vanilla Modern CSS3 with CSS variables and responsive design
- **HTTP Client**: Native Fetch API with Basic Auth header injection

### Hosting & Infrastructure
- **Backend Hosting**: Render (Docker Multi-stage deployment with Temurin 17 JRE Alpine)
- **Database Hosting**: Render Managed PostgreSQL
- **Frontend Hosting**: Vercel

---

## 4. Architecture & Project Structure

### Architecture Diagram
```mermaid
graph TD
    subgraph Frontend [Client Tier - Vercel / Local]
        Browser[React 19 Single Page App]
    end

    subgraph Backend [Backend Tier - Render / Local Spring Boot]
        Sec[Spring Security Filter Chain<br/>HTTP Basic Auth & RBAC]
        Ctrl[REST Controllers<br/>SlotController | BookingController | AdminBookingController]
        Svc[Service Layer<br/>BookingService | SlotService]
        Tx[Transaction Boundary<br/>@Transactional Isolation]
        Repo[Spring Data Repositories<br/>SlotRepository | BookingRepository | UserRepository]
        PostProc[DatabaseUrlEnvironmentPostProcessor<br/>Render URL Normalization]
    end

    subgraph Database [Database Tier - PostgreSQL]
        PG[(PostgreSQL 14+<br/>booknow_db)]
        LockTable[Row Lock: slots FOR NO KEY UPDATE]
    end

    Browser -->|HTTP Basic Auth REST Calls| Sec
    Sec --> Ctrl
    Ctrl --> Svc
    Svc --> Tx
    Tx --> Repo
    Repo -->|Pessimistic Lock Query| LockTable
    LockTable --> PG
    PostProc -.->|Bootstraps JDBC Config| PG
```

### Request Flow
```
Client Request -> Spring Security (HTTP Basic Auth) -> Controller (DTO Validation) -> Service Layer (@Transactional) -> Repository (Pessimistic Lock) -> PostgreSQL Engine -> Response DTO
```

### Repository Directory Tree
```
BookNow/
├── .env.example                               # Root-level environment template
├── .gitignore                                 # Git exclusions for secrets and build artifacts
├── README.md                                  # Complete project documentation
├── backend/
│   ├── .env.example                           # Backend-specific environment template
│   ├── .gitignore                             # Backend gitignore
│   ├── Dockerfile                             # Multi-stage production container for Render
│   ├── mvnw / mvnw.cmd                        # Maven wrapper scripts
│   ├── pom.xml                                # Maven project object model (Java 17, Spring Boot 4.1.1)
│   ├── README.md                              # Backend quick-start guide
│   └── src/
│       ├── main/
│       │   ├── java/com/example/bookingsystem/
│       │   │   ├── BookingSystemApplication.java        # Main class with startup profile & DB logging
│       │   │   ├── config/
│       │   │   │   └── DatabaseUrlEnvironmentPostProcessor.java # Auto-converts Render postgresql:// URLs
│       │   │   ├── controller/
│       │   │   │   ├── AdminBookingController.java      # Admin cancellation endpoint
│       │   │   │   ├── BookingController.java           # User booking creation, list, & cancellation
│       │   │   │   └── SlotController.java              # Admin slot creation & public slot retrieval
│       │   │   ├── dto/
│       │   │   │   ├── request/
│       │   │   │   │   ├── CreateBookingRequest.java    # SlotId payload with validation
│       │   │   │   │   └── CreateSlotRequest.java       # StartTime & EndTime validation
│       │   │   │   └── response/
│       │   │   │       ├── BookingResponse.java         # Booking entity representation
│       │   │   │       ├── ErrorResponse.java           # Standardized error payload
│       │   │   │       └── SlotResponse.java            # Slot details with activeBookingId
│       │   │   ├── entity/
│       │   │   │   ├── Booking.java                     # Booking entity (ManyToOne Slot & User)
│       │   │   │   ├── BookingStatus.java               # ACTIVE, CANCELLED
│       │   │   │   ├── Role.java                        # USER, ADMIN
│       │   │   │   ├── Slot.java                        # Slot entity (startTime, endTime, status)
│       │   │   │   ├── SlotStatus.java                  # AVAILABLE, BOOKED
│       │   │   │   └── User.java                        # User entity (username unique, password, role)
│       │   │   ├── exception/
│       │   │   │   ├── BookingNotFoundException.java
│       │   │   │   ├── GlobalExceptionHandler.java      # Centralized @RestControllerAdvice
│       │   │   │   ├── InvalidBookingStateException.java
│       │   │   │   ├── SlotAlreadyBookedException.java
│       │   │   │   ├── SlotNotFoundException.java
│       │   │   │   └── UnauthorizedBookingException.java
│       │   │   ├── repository/
│       │   │   │   ├── BookingRepository.java
│       │   │   │   ├── SlotRepository.java          # Includes pessimistic write lock query
│       │   │   │   └── UserRepository.java
│       │   │   ├── security/
│       │   │   │   ├── CustomUserDetails.java
│       │   │   │   ├── CustomUserDetailsService.java
│       │   │   │   ├── DataInitializer.java         # Seeds demo accounts (admin, user1, user2)
│       │   │   │   └── SecurityConfig.java          # Spring Security Filter Chain & CORS
│       │   │   └── service/
│       │   │       ├── BookingService.java          # Concurrency locking & booking lifecycle
│       │   │       └── SlotService.java             # Slot creation and retrieval
│       │   └── resources/
│       │       ├── application.properties           # Common settings shared across environments
│       │       ├── application-local.properties     # Local development PostgreSQL configuration
│       │       ├── application-prod.properties      # Production Render PostgreSQL configuration
│       │       ├── db/migration/
│       │       │   └── V1__init_schema.sql          # Ready-to-use PostgreSQL schema script
│       │       └── META-INF/
│       │           ├── spring.factories
│       │           └── spring/org.springframework.boot.env.EnvironmentPostProcessor.imports
│       └── test/
│           ├── java/com/example/bookingsystem/
│           │   ├── BookingSystemApplicationTests.java
│           │   ├── config/
│           │   │   └── DatabaseUrlEnvironmentPostProcessorTest.java # Tests Render URL parser
│           │   ├── controller/
│           │   │   ├── BookingApiTest.java          # Full MockMvc booking integration tests
│           │   │   ├── CancelBookingApiTest.java    # Full MockMvc cancellation integration tests
│           │   │   └── SlotApiTest.java             # Slot creation & validation tests
│           │   ├── security/
│           │   │   └── SecurityConfigTest.java      # RBAC authorization test suite
│           │   └── service/
│           │       ├── BookingConcurrencyTest.java  # Multi-threaded 10-user race test
│           │       └── CancelBookingConcurrencyTest.java # Simultaneous cancel & book race test
│           └── resources/
│               └── application.properties           # In-memory test isolation configuration
└── frontend/
    ├── package.json                                 # React 19 & Vite 8 configuration
    ├── vite.config.js                               # Vite development server config
    ├── .env.example                                 # Frontend API URL template
    └── src/
        ├── App.jsx                                  # Route controller & layout
        ├── components/
        │   ├── Header.jsx                           # Navigation bar with user badge & logout
        │   ├── LoginModal.jsx                       # HTTP Basic credentials dialog
        │   ├── SlotCard.jsx                         # Interactive slot card with state badges
        │   └── SlotList.jsx                         # Grid of slot cards
        ├── context/
        │   └── AuthContext.jsx                      # React context for auth credentials
        ├── pages/
        │   ├── AdminDashboard.jsx                   # Admin slot creation & admin cancel view
        │   ├── BookingPage.jsx                      # User slot booking & personal bookings
        │   └── ExplorePage.jsx                      # Public marketing view
        └── services/
            └── api.js                               # Centralized API client with Basic Auth
```

---

## 5. Features & Business Rules

### 1. Authentication & Role-Based Authorization
- Stateless **HTTP Basic Authentication** (`Authorization: Basic base64(username:password)`).
- Two roles: `ROLE_USER` and `ROLE_ADMIN`.
- Server derives user identity strictly from the authenticated Spring Security Principal (`principal.getName()`). Clients are never permitted to pass a client-supplied `userId` to create or cancel bookings.

### 2. Slot Management
- **Admin Slot Creation**: Only `ADMIN` can create slots (`POST /slots`).
- **Validation**:
  - `startTime` and `endTime` are required (`@NotNull`).
  - `startTime` must strictly precede `endTime`.
- **Initial Status**: New slots are always created with `status = AVAILABLE`.

### 3. Slot Retrieval
- Accessible to both `USER` and `ADMIN` (`GET /slots`).
- If a slot is `BOOKED`, the response includes its `activeBookingId` so authorized users/admins can perform cancellations.

### 4. Booking Creation
- Only `USER` can book slots (`POST /bookings`).
- Admins are forbidden from booking slots (HTTP `403 Forbidden`).
- A slot cannot be booked unless its status is `AVAILABLE`.
- Successfully booking a slot marks the slot as `BOOKED`, persists an `ACTIVE` booking record, and returns HTTP `201 Created`.

### 5. Booking Cancellation
- **User Cancellation** (`POST /bookings/{id}/cancel`):
  - A user can only cancel their **own** booking. Attempting to cancel another user's booking returns HTTP `403 Forbidden`.
  - The booking status transitions from `ACTIVE` to `CANCELLED`.
  - The associated slot status transitions back from `BOOKED` to `AVAILABLE`.
  - Already cancelled bookings cannot be cancelled again (returns HTTP `409 Conflict`).
- **Admin Cancellation** (`POST /admin/bookings/{id}/cancel`):
  - Administrators can cancel **any** active booking regardless of ownership.

---

## 6. Concurrency & Race-Condition Deep Dive

### The Concurrency Problem
Consider a single available slot with `id = 101`. Two users, Alice and Bob, submit booking requests at the exact same millisecond:

```
Timeline:
T0: Slot 101 status is AVAILABLE in PostgreSQL.
T1: Alice sends POST /bookings {"slotId": 101}.
T1: Bob sends POST /bookings {"slotId": 101}.
```

If the application were implemented without database locking:
1. Alice's thread executes `SELECT * FROM slots WHERE id = 101` &rarr; sees `AVAILABLE`.
2. Bob's thread executes `SELECT * FROM slots WHERE id = 101` &rarr; sees `AVAILABLE`.
3. Alice's thread sets slot to `BOOKED` and saves a booking.
4. Bob's thread sets slot to `BOOKED` and saves a second booking.
5. **Double-Booking Result**: Slot 101 has two active bookings.

### The BookNow Solution: Pessimistic Row Locking
In [`SlotRepository.java`](file:///c:/Users/Admin/Desktop/BookNow/backend/src/main/java/com/example/bookingsystem/repository/SlotRepository.java):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Slot s WHERE s.id = :id")
Optional<Slot> findByIdWithPessimisticWriteLock(@Param("id") Long id);
```

When [`BookingService.createBooking()`](file:///c:/Users/Admin/Desktop/BookNow/backend/src/main/java/com/example/bookingsystem/service/BookingService.java) executes within its `@Transactional` boundary, Hibernate sends the following SQL query to PostgreSQL:
```sql
SELECT s.id, s.start_time, s.end_time, s.status
FROM slots s
WHERE s.id = ?
FOR NO KEY UPDATE;
```

#### Step-by-Step Execution Sequence:
1. **Thread 1 (Alice)** acquires PostgreSQL's row-level lock on Slot `101`.
2. **Thread 2 (Bob)** attempts to acquire the lock on Slot `101`. Because Alice holds the row lock, PostgreSQL **blocks Thread 2** at the database engine level until Alice's transaction completes.
3. Alice inspects the slot state: `status == AVAILABLE`.
4. Alice updates `slot.status = BOOKED`, inserts a new `Booking` row with status `ACTIVE`, commits the transaction, and releases the lock. Alice receives **HTTP 201 Created**.
5. PostgreSQL unblocks Thread 2 (Bob) and grants it the lock.
6. Bob's transaction reads the **freshly committed row state**: `status == BOOKED`.
7. `BookingService` evaluates `if (slot.getStatus() == SlotStatus.BOOKED)` and throws `SlotAlreadyBookedException("Slot is already booked")`.
8. Spring's `GlobalExceptionHandler` intercepts the exception and returns **HTTP 409 Conflict** to Bob.
9. **Guaranteed Outcome**: Exactly 1 active booking exists in the database.

---

## 7. Prerequisites & Local Setup

### Prerequisites
- **Java Development Kit (JDK)**: Java 17 or higher (Eclipse Temurin, OpenJDK, or Oracle JDK).
- **PostgreSQL**: Version 14 or higher installed and running locally on port `5432`.
- **Node.js**: Version 18+ and `npm` (for the React frontend).
- **Git**: Installed and configured.

### Step-by-Step Setup

#### Step 1: Clone the Repository
```bash
git clone https://github.com/gaurav22iyou/Look-Book.git BookNow
cd BookNow
```

#### Step 2: Create the Local PostgreSQL Database
Open PostgreSQL interactive shell (`psql`) or pgAdmin:
```sql
CREATE DATABASE booknow_db;
```
Verify the database exists:
```sql
\l
```

#### Step 3: Configure Backend Environment Variables
Navigate to the `backend` folder and create a `.env` file based on `.env.example`:
```bash
cd backend
cp .env.example .env
```
Ensure your `.env` contains your actual local PostgreSQL password:
```properties
SPRING_PROFILES_ACTIVE=local
LOCAL_DB_HOST=localhost
LOCAL_DB_PORT=5432
LOCAL_DB_NAME=booknow_db
LOCAL_DB_USERNAME=postgres
LOCAL_DB_PASSWORD=your_local_postgres_password
LOCAL_DB_URL=jdbc:postgresql://localhost:5432/booknow_db
```

#### Step 4: Run the Backend with the `local` Profile
**Windows PowerShell:**
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```
**Linux / macOS:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Upon boot, the application logs:
```
=================================================================
Application 'booknow' is running!
Active Profile(s): [local]
Database URL:      jdbc:postgresql://localhost:5432/booknow_db
Server Port:       8080
=================================================================
```

#### Step 5: Start the Frontend Application
In a separate terminal:
```bash
cd frontend
npm install
npm run dev
```
The Vite development server will start at:
```
http://localhost:5173
```

---

## 8. Environment Configuration

### Local Profile (`application-local.properties`)
Activated with `SPRING_PROFILES_ACTIVE=local`. Configured for local development with detailed Hibernate SQL logging and automatic schema updates (`ddl-auto=update`):

| Property Name | Environment Variable Override | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `spring.datasource.url` | `LOCAL_DB_URL` | `jdbc:postgresql://localhost:5432/booknow_db` | PostgreSQL JDBC connection URL |
| `spring.datasource.username` | `LOCAL_DB_USERNAME` | `postgres` | Local PostgreSQL username |
| `spring.datasource.password` | `LOCAL_DB_PASSWORD` | `""` (empty) | Local PostgreSQL password |
| `spring.jpa.hibernate.ddl-auto` | — | `update` | Automatically synchronizes entity schema |
| `spring.jpa.show-sql` | — | `true` | Prints executed SQL queries to the console |

### Production Profile (`application-prod.properties`)
Activated on Render with `SPRING_PROFILES_ACTIVE=prod`. Configured for high-throughput production deployment with connection pooling:

| Property Name | Environment Variable Override | Description |
| :--- | :--- | :--- |
| `spring.datasource.url` | `DB_URL` *(or `DATABASE_URL`)* | Managed PostgreSQL connection URL |
| `spring.datasource.username` | `DB_USERNAME` | Production database user |
| `spring.datasource.password` | `DB_PASSWORD` | Production database password |
| `spring.datasource.hikari.maximum-pool-size` | `DB_POOL_MAX_SIZE` | Connection pool cap (default: `10`) |
| `app.cors.allowed-origins` | `APP_CORS_ALLOWED_ORIGINS` | Permitted frontend URLs (e.g. Vercel) |

### Render Connection URL Auto-Conversion
Render provides PostgreSQL connection strings starting with `postgresql://` or `postgres://` (e.g. `postgresql://user:pass@dpg-xxx-a:5432/dbname`).
Java JDBC drivers do not accept these URLs directly. BookNow includes [`DatabaseUrlEnvironmentPostProcessor.java`](file:///c:/Users/Admin/Desktop/BookNow/backend/src/main/java/com/example/bookingsystem/config/DatabaseUrlEnvironmentPostProcessor.java), which automatically parses Render connection URLs and translates them into standard `jdbc:postgresql://` format with parsed credentials.

---

## 9. Demo Login Credentials

The application automatically seeds three demo accounts on startup via [`DataInitializer.java`](file:///c:/Users/Admin/Desktop/BookNow/backend/src/main/java/com/example/bookingsystem/security/DataInitializer.java) if they do not already exist in the database:

| Account | Username | Default Password | Role | Permissions |
| :--- | :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `adminpass` | `ROLE_ADMIN` | Create slots, view slots, cancel any booking |
| **User 1** | `user1` | `user1pass` | `ROLE_USER` | View slots, book available slots, cancel own bookings |
| **User 2** | `user2` | `user2pass` | `ROLE_USER` | View slots, book available slots, cancel own bookings |

> [!IMPORTANT]
> These credentials are strictly intended for local demonstration, integration testing, and evaluation. In a production environment, override these passwords via environment variables (`ADMIN_PASSWORD`, `USER1_PASSWORD`, `USER2_PASSWORD`) or manage users through a dedicated authentication identity provider.

---

## 10. Complete Application Usage Guide

### Scenario A: Log in as Admin and Create a Slot

#### Via the Web UI:
1. Open your browser to `http://localhost:5173`.
2. Click **Login** in the header.
3. Enter username `admin` and password `adminpass`.
4. The application automatically redirects to the **Admin Dashboard** (`/admin`).
5. In the **Create Slot** section:
   - Select a **Start Time** (e.g., Tomorrow at `10:00 AM`).
   - Select an **End Time** (e.g., Tomorrow at `11:00 AM`).
6. Click **Create Slot**.
7. Confirm that the new slot appears in the slots list with status **AVAILABLE** and record the assigned `Slot ID` (e.g., `1`).

#### Via cURL / Terminal:
```bash
curl -X POST http://localhost:8080/slots \
  -u admin:adminpass \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2026-10-25T10:00:00",
    "endTime": "2026-10-25T11:00:00"
  }'
```
**Expected Response (HTTP 201 Created):**
```json
{
  "id": 1,
  "startTime": "2026-10-25T10:00:00",
  "endTime": "2026-10-25T11:00:00",
  "status": "AVAILABLE",
  "activeBookingId": null
}
```

---

### Scenario B: Log in as User 1 on Device 1
1. Open a standard browser window (Device 1 / Browser 1) at `http://localhost:5173`.
2. Click **Login**, enter `user1` / `user1pass`.
3. The UI redirects to the **Booking Portal** (`/booking`).
4. Locate Slot `1` created by the admin. Verify the status badge displays **AVAILABLE**.
5. Keep this window open and ready.

---

### Scenario C: Log in as User 2 on Device 2
1. Open an **Incognito / Private Window** (or a separate browser/device) at `http://localhost:5173`.
   *(Using an incognito window is necessary because HTTP Basic Auth headers are cached per browser session).*
2. Click **Login**, enter `user2` / `user2pass`.
3. Locate the identical Slot `1`.
4. Verify the status badge also displays **AVAILABLE**.

---

### Scenario D: Test Concurrent Booking Requests

#### Manual Concurrency Test:
1. Position the two browser windows side by side.
2. In Window 1 (User 1), prepare to click **Book Now** on Slot 1.
3. In Window 2 (User 2), prepare to click **Book Now** on Slot 1.
4. Click **Book Now** in both windows as simultaneously as possible.
5. **Observed Result**:
   - **User 1**: Receives a green success toast: *"Booking confirmed successfully!"*. Slot badge updates to **BOOKED**.
   - **User 2**: Receives an error notification: *"This slot was just booked by another user. Please choose another slot."* (HTTP 409 Conflict).

#### True Automated Multi-Threaded Concurrency Test:
Manual browser clicks cannot guarantee nanosecond-level packet delivery to prove race condition safety. The repository includes an automated integration test: [`BookingConcurrencyTest.java`](file:///c:/Users/Admin/Desktop/BookNow/backend/src/test/java/com/example/bookingsystem/service/BookingConcurrencyTest.java).

This test spins up **10 concurrent threads** using `CountDownLatch`, releasing all 10 threads at the exact same instant to book the same slot:
```bash
# Run the automated concurrency test against local PostgreSQL
cd backend
$env:LOCAL_DB_PASSWORD="your_password"
.\mvnw.cmd test "-Dspring.profiles.active=local" "-Dtest=BookingConcurrencyTest"
```
**Test Assertion Guarantee:**
- `assertEquals(1, successCount.get(), "Exactly ONE booking should succeed")`
- `assertEquals(SlotStatus.BOOKED, updatedSlot.getStatus())`
- `assertEquals(1, activeBookings, "There should be exactly one ACTIVE booking")`

---

### Scenario E: Verify Final Database State
Open `psql` to inspect the PostgreSQL database and verify consistency:

```sql
-- Connect to the local database
\c booknow_db

-- 1. Check Slot Status (Must be BOOKED)
SELECT id, start_time, end_time, status FROM slots;

-- 2. Verify that only ONE active booking exists
SELECT id, slot_id, user_id, status, created_at FROM bookings WHERE status = 'ACTIVE';

-- 3. Confirm which user won the race
SELECT b.id AS booking_id, u.username, b.status, b.created_at
FROM bookings b
JOIN users u ON b.user_id = u.id;
```

---

## 11. API Documentation

All endpoints require HTTP Basic Authentication (`Authorization: Basic base64(username:password)`).

| Method | Endpoint | Required Role | Request Body | Success Response | Error Codes | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/slots` | `ROLE_ADMIN` | JSON `{ "startTime", "endTime" }` | `201 Created` | `400`, `401`, `403` | Create a new bookable slot |
| `GET` | `/slots` | `ROLE_USER`, `ROLE_ADMIN` | None | `200 OK` (Array) | `401` | Retrieve all slots |
| `POST` | `/bookings` | `ROLE_USER` | JSON `{ "slotId" }` | `201 Created` | `400`, `401`, `403`, `404`, `409` | Book an available slot |
| `GET` | `/bookings` | `ROLE_USER` | None | `200 OK` (Array) | `401`, `403` | Get current user's active bookings |
| `POST` | `/bookings/{id}/cancel` | `ROLE_USER` | None | `200 OK` | `401`, `403`, `404`, `409` | Cancel user's own booking |
| `POST` | `/admin/bookings/{id}/cancel` | `ROLE_ADMIN` | None | `200 OK` | `401`, `403`, `404`, `409` | Admin cancels any booking |

### Example cURL Commands

#### 1. Admin Creates a Slot
```bash
curl -X POST http://localhost:8080/slots \
  -u admin:adminpass \
  -H "Content-Type: application/json" \
  -d '{"startTime": "2026-10-30T09:00:00", "endTime": "2026-10-30T10:00:00"}'
```

#### 2. User Retrieves Available Slots
```bash
curl -X GET http://localhost:8080/slots \
  -u user1:user1pass
```

#### 3. User Books a Slot
```bash
curl -X POST http://localhost:8080/bookings \
  -u user1:user1pass \
  -H "Content-Type: application/json" \
  -d '{"slotId": 1}'
```

#### 4. User Cancels Own Booking
```bash
curl -X POST http://localhost:8080/bookings/1/cancel \
  -u user1:user1pass
```

#### 5. Admin Cancels Any Booking
```bash
curl -X POST http://localhost:8080/admin/bookings/1/cancel \
  -u admin:adminpass
```

---

## 12. Error Handling & Standardized Responses

All errors are intercepted by [`GlobalExceptionHandler.java`](file:///c:/Users/Admin/Desktop/BookNow/backend/src/main/java/com/example/bookingsystem/exception/GlobalExceptionHandler.java) and returned in a consistent schema:

```json
{
  "timestamp": "2026-10-20T20:30:15.123",
  "status": 409,
  "error": "Conflict",
  "message": "Slot is already booked",
  "path": "/bookings"
}
```

### Standard Status Codes:
- **`400 Bad Request`**: Validation errors (e.g., missing `slotId`, missing `startTime`, `startTime` after `endTime`, malformed JSON).
- **`401 Unauthorized`**: Missing or invalid HTTP Basic credentials.
- **`403 Forbidden`**: Role violation (e.g., user attempting to access admin endpoints, admin attempting to book slots, or user attempting to cancel another user's reservation).
- **`404 Not Found`**: Target slot or booking ID does not exist.
- **`409 Conflict`**: Target slot is already booked, or attempting to cancel an already cancelled booking.

---

## 13. Testing, Coverage & Quality

### Test Suite Summary
The backend contains **48 automated unit, integration, and concurrency tests**:

| Test Class | Category | Test Count | Key Invariants Verified |
| :--- | :--- | :---: | :--- |
| `BookingConcurrencyTest` | Concurrency | 1 | 10 simultaneous threads competing for 1 slot; exactly 1 succeeds |
| `CancelBookingConcurrencyTest` | Concurrency | 1 | Race condition between simultaneous cancel and book operations |
| `BookingApiTest` | Integration | 8 | User booking lifecycle, isolation between users, status assertions |
| `CancelBookingApiTest` | Integration | 11 | Ownership checks, admin cancel privileges, double cancel guard |
| `SlotApiTest` | Integration | 10 | Admin creation, validation (missing/invalid dates), retrieval |
| `SecurityConfigTest` | Security | 11 | Role-based URL protections, unauthenticated rejection |
| `DatabaseUrlEnvironmentPostProcessorTest` | Unit | 4 | Render URL conversion, credential extraction, query parameter parsing |
| `BookingSystemApplicationTests` | Context | 1 | Spring ApplicationContext verification |
| **Total** | | **48** | **0 Failures, 0 Errors, 0 Skipped (100% Pass Rate)** |

### Running the Tests

#### 1. Run Complete Test Suite (In-Memory Isolation)
```bash
cd backend
.\mvnw.cmd test
```

#### 2. Run Full Build with JaCoCo Coverage Enforcement
```bash
.\mvnw.cmd clean verify
```
The JaCoCo coverage report is generated at:
```
backend/target/site/jacoco/index.html
```

#### 3. Run Tests against Live Local PostgreSQL
```bash
$env:LOCAL_DB_PASSWORD="your_postgres_password"
.\mvnw.cmd test -Dspring.profiles.active=local
```

### SonarQube Quality Analysis
To run SonarQube static code analysis:
```bash
.\mvnw.cmd clean verify sonar:sonar \
  -Dsonar.host.url=$SONAR_HOST_URL \
  -Dsonar.login=$SONAR_TOKEN \
  -Dsonar.projectKey=booknow
```

---

## 14. Production Deployment Guide

### A. Deploy PostgreSQL on Render
1. In the [Render Dashboard](https://dashboard.render.com/), click **New +** &rarr; **PostgreSQL**.
2. Name the database `booknow-db`.
3. Set Database Name to `booknow_db`.
4. Copy the **Internal Database URL** (e.g., `postgresql://booknow_user:pass@dpg-xxx-a:5432/booknow_db`).

### B. Deploy Spring Boot Backend on Render
1. In Render, click **New +** &rarr; **Web Service**.
2. Connect your GitHub repository.
3. Select **Docker** as the Runtime environment (Render uses [`backend/Dockerfile`](file:///c:/Users/Admin/Desktop/BookNow/backend/Dockerfile)).
4. Set Docker Context Directory to `backend`.
5. In **Environment Variables**, add:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DB_URL`: *(Paste your Render PostgreSQL connection string or internal JDBC URL)*
   - `APP_CORS_ALLOWED_ORIGINS`: `https://your-frontend.vercel.app`
6. Click **Deploy Web Service**.

### C. Deploy Frontend on Vercel
1. In [Vercel](https://vercel.com/), click **Add New Project** and select your repository.
2. Set Root Directory to `frontend`.
3. Framework Preset: **Vite**.
4. In **Environment Variables**, add:
   - `VITE_API_URL`: `https://your-backend.onrender.com`
5. Click **Deploy**.

---

## 15. Troubleshooting Guide

| Issue | Root Cause | Solution |
| :--- | :--- | :--- |
| `FATAL: password authentication failed for user "postgres"` | Incorrect `LOCAL_DB_PASSWORD` | Verify your local PostgreSQL password. Set `$env:LOCAL_DB_PASSWORD='your_password'`. |
| `FATAL: database "booknow_db" does not exist` | Database not yet created | Run `CREATE DATABASE booknow_db;` in `psql`. |
| `Connection refused: connect` on port 5432 | PostgreSQL service is stopped | Start the PostgreSQL service in Windows Services (`Start-Service postgresql-x64-16`). |
| `Port 8080 was already in use` | Another process is bound to port 8080 | Terminate the occupying process or set `PORT=8081` in `.env`. |
| `401 Unauthorized` on API call | Invalid or missing credentials | Ensure the `Authorization: Basic <base64>` header is provided with valid demo credentials. |
| `403 Forbidden` when creating slot | Non-admin user attempted admin endpoint | Log in with the `admin` account (`adminpass`). |
| `409 Conflict: Slot is already booked` | Slot already has an active booking | This is expected behavior under concurrency or when booking a taken slot. Select another slot. |
| CORS Error in browser console | Frontend domain not permitted by backend | Add your frontend URL to `app.cors.allowed-origins` in `application.properties` or set `APP_CORS_ALLOWED_ORIGINS`. |

---

## 16. Security & Limitations

### Security Architecture
- **Stateless HTTP Basic**: Configured for simplicity, reliability, and easy testing. For large enterprise scale, this can be transitioned to OAuth2/JWT tokens with an authorization server.
- **Principal Derivation**: User identities are derived strictly from server-side security context, protecting against user-impersonation injection attacks.
- **Credential Protection**: Real passwords and production database connection strings are excluded via `.gitignore` and must never be committed to version control.

### Limitations
- **Slot Time Overlap**: The current model does not prevent an admin from creating overlapping time intervals (e.g., 10:00-11:00 and 10:30-11:30). Each slot is an independent entity.
- **In-Memory Sessions in UI**: The React frontend caches the Basic Auth credentials in React state/memory for the duration of the page session.

---

## 17. Contribution & License

### Contribution Workflow
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/enhanced-booking`).
3. Commit your changes (`git commit -m "Add slot recurrence feature"`).
4. Run full verification:
   ```bash
   cd backend
   .\mvnw.cmd clean verify
   ```
5. Push to your branch and open a Pull Request.

### License
No license file is currently provided. All rights reserved.
