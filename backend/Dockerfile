# Build Stage: Compile and Package Spring Boot Application
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml and download dependencies first for efficient layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage: Lightweight Java 21 JRE Execution Environment
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root system user and group for container security
RUN addgroup -S expenseguard && adduser -S expenseguard -G expenseguard

# Copy executable JAR from build stage
COPY --from=builder /app/target/expenseguard-backend-0.0.1-SNAPSHOT.jar app.jar

# Create receipt storage directory and set directory permissions
RUN mkdir -p /app/uploads/receipts && chown -R expenseguard:expenseguard /app

# Switch to non-root user
USER expenseguard

# Expose backend application port
EXPOSE 8081

# Container Health Check
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8081/api/v1/health || exit 1

# Execute Spring Boot JAR
ENTRYPOINT ["java", "-jar", "app.jar"]
