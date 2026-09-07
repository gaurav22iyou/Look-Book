## ---------- Multi-stage Dockerfile for Render deployment ----------
# Build stage: use Maven + JDK 17 to compile the Spring Boot app
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
# Ensure the wrapper is executable
RUN chmod +x ./mvnw
# Download dependencies (cache layer)
RUN ./mvnw dependency:go-offline -B
# Copy source code
COPY src ./src
# Build the jar without tests (tests will be run separately)
RUN ./mvnw -DskipTests clean package

# Runtime stage: lightweight JRE 17 image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar
# Expose port (Render provides $PORT at runtime)
EXPOSE 8080
# Use the PORT environment variable if set, default to 8080 (handled by Spring Boot config)

# Entry point to run the Spring Boot application
ENTRYPOINT ["java","-jar","/app/app.jar"]
