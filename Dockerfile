# Multi-stage Docker build for Spring Boot on Render
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy Maven wrapper and POM to leverage layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies offline
RUN ./mvnw dependency:go-offline -B || true

# Copy source and build executable JAR (skip tests during deployment)
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Lightweight runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create non-root system user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE ${PORT}

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
