# Runtime-only image. Build the jar locally first:
#   ./gradlew bootJar -x test
# The build produces build/libs/SuperManagerofHeresyforIdiots.jar.
# Then: docker compose build app

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache ca-certificates curl && \
    update-ca-certificates && \
    addgroup -S app && adduser -S -G app -h /app app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -Dhttps.protocols=TLSv1.2,TLSv1.3 \
               -Djava.security.egd=file:/dev/./urandom"

COPY --chown=app:app build/libs/SuperManagerofHeresyforIdiots.jar app.jar

EXPOSE 8080

USER app

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
