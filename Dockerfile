# Multi-stage build for PILOTO F1 - Build both modules
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# First, build cdi-kernel-phase1
COPY pom.xml .
COPY src ./src
RUN mvn clean install -DskipTests -B

# Then, build cdi-api-gateway
WORKDIR /build/gateway
COPY cdi-api-gateway/pom.xml .
COPY cdi-api-gateway/src ./src
RUN mvn clean package -DskipTests -B

# Production stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy built jar from gateway build
COPY --from=builder /build/gateway/target/*.jar app.jar

# Create data directories
RUN mkdir -p /app/.piloto-data/events /app/.piloto-data/memory

# Expose port
EXPOSE 8080

# Health check - usar root endpoint ya que actuator no está expuesto
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
