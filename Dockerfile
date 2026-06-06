# syntax=docker/dockerfile:1.6

# =============================================================================
# Stage 1 — build the Spring Boot fat jar with Maven.
# Uses the official Maven image so we don't have to deal with mvnw line-endings
# (the project was built on Windows, so the shell wrapper may have CRLF).
# =============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Pull dependencies first so they cache across source-only rebuilds.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Then the source.
COPY src ./src

# Skip tests inside the image build — run them in CI instead.
RUN mvn -B -q clean package -DskipTests \
    && cp target/*.jar app.jar

# =============================================================================
# Stage 2 — slim runtime image with just the JRE and the jar.
# =============================================================================
FROM eclipse-temurin:17-jre
WORKDIR /app

# Run as a non-root user.
RUN groupadd --system app \
 && useradd  --system --gid app --home /app --no-create-home app \
 && mkdir -p /app/uploads \
 && chown -R app:app /app

USER app

COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar

# Defaults. Override any of these at `docker run -e ...` or in docker-compose.
ENV SERVER_PORT=8081 \
    SPRING_PROFILES_ACTIVE=dev \
    UPLOADS_ROOT=/app/uploads \
    JAVA_OPTS=""

EXPOSE 8081

# Use the shell form so $JAVA_OPTS can be injected at runtime (tunables, heap sizes).
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
