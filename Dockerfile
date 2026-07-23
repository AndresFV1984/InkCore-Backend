# InkCore-backend — imagen runtime (Java 17)
# Imagen Docker Hub: bayronindicore/inkcore-backend
# Tag de versión = v + día.mes (ej. v22.07). Preferir: .\scripts\docker\push-hub.ps1
#
# Manual:
#   ./mvnw "-Dmaven.test.skip=true" package
#   $env:IMAGE_TAG = Get-Date -Format "vdd.MM"
#   docker compose build && docker push "bayronindicore/inkcore-backend:$env:IMAGE_TAG"

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system inkcore \
    && useradd --system --gid inkcore --home-dir /app --shell /usr/sbin/nologin inkcore

# JAR generado en el host (evita descargas Maven inestables dentro del build)
COPY target/inkcore-backend-*.jar /app/app.jar
RUN chown inkcore:inkcore /app/app.jar

USER inkcore

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=docker \
    SERVER_PORT=8091 \
    APP_CONTEXT=/InkCore-backend

EXPOSE 8091

HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=8 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}${APP_CONTEXT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
