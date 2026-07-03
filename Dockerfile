# syntax=docker/dockerfile:1
# Multi-stage build: compiles the jar inside Docker (no local ./gradlew needed),
# then ships a slim JRE runtime image.

# ── Stage 1: build the bootJar with the project's own Gradle wrapper ──
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy the wrapper + build scripts first so dependency resolution is cached
# independently of source changes (better layer reuse on rebuilds).
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Now copy the sources and produce the bootJar (fast tests only are wired into
# `test`; we skip them here since CI runs them — keep the image build focused).
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar -x test

# ── Stage 2: runtime ──
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache ca-certificates curl && \
    update-ca-certificates && \
    addgroup -S app && adduser -S -G app -h /app app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Dhttps.protocols=TLSv1.2,TLSv1.3 \
               -Djava.security.egd=file:/dev/./urandom"

COPY --from=build --chown=app:app /workspace/build/libs/SuperManagerofHeresyforIdiots.jar app.jar

EXPOSE 8080

USER app

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
