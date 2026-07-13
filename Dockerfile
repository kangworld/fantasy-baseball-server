# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY src src

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl openssl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && curl --fail --silent --show-error \
      https://truststore.pki.rds.amazonaws.com/ap-northeast-2/ap-northeast-2-bundle.pem \
      -o /tmp/rds-bundle.pem \
    && cp "$JAVA_HOME/lib/security/cacerts" /app/combined-truststore \
    && awk '/-----BEGIN CERTIFICATE-----/{file=sprintf("/tmp/rds-ca-%02d.pem",++n)} file{print > file} /-----END CERTIFICATE-----/{close(file); file=""}' /tmp/rds-bundle.pem \
    && for cert in /tmp/rds-ca-*.pem; do \
      alias="rds-$(basename "$cert" .pem)"; \
      keytool -importcert -noprompt -alias "$alias" -file "$cert" \
        -keystore /app/combined-truststore -storepass changeit; \
    done \
    && rm -f /tmp/rds-bundle.pem /tmp/rds-ca-*.pem \
    && chown app:app /app/combined-truststore \
    && chmod 0444 /app/combined-truststore

COPY --from=build /workspace/build/libs/*.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
