# BookNow Backend

Spring Boot backend for BookNow — a high-concurrency booking platform with robust race-condition protection.

## Quick Start (Local Development)

### 1. Prerequisites
- Java 17+
- PostgreSQL 14+ installed and running locally on port 5432

### 2. Create the Database
In PostgreSQL (`psql`):
```sql
CREATE DATABASE booknow_db;
```

### 3. Configure Environment Variables
Copy `.env.example` to `.env` or set environment variables:
```bash
SPRING_PROFILES_ACTIVE=local
LOCAL_DB_URL=jdbc:postgresql://localhost:5432/booknow_db
LOCAL_DB_USERNAME=postgres
LOCAL_DB_PASSWORD=your_postgres_password
```

### 4. Run the Application
```bash
# Windows
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local

# Linux / macOS
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 5. Run Tests
```bash
# Run isolated test suite (in-memory test isolation)
.\mvnw.cmd clean test

# Run tests directly against local PostgreSQL
$env:SPRING_PROFILES_ACTIVE="local"; $env:LOCAL_DB_PASSWORD="your_password"; .\mvnw.cmd test -Dspring.profiles.active=local
```
