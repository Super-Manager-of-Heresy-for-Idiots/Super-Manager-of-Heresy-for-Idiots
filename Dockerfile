# Только для запуска, без сборки внутри контейнера
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Устанавливаем CA сертификаты и curl для healthcheck
RUN apk add --no-cache ca-certificates curl && \
    update-ca-certificates

# Настройки JVM для runtime
ENV JAVA_OPTS="-Dhttps.protocols=TLSv1.2,TLSv1.3 \
               -Djava.security.egd=file:/dev/./urandom"

# Копируем готовый JAR (который собрали на хосте)
COPY build/libs/*.jar app.jar

EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]