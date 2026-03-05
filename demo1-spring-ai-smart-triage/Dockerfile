# Build stage — no gradle wrapper in project, use official gradle image
FROM gradle:8.11-jdk21-alpine AS builder

WORKDIR /app

# Copy build files first for dependency caching
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src src

# Build
RUN gradle bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
