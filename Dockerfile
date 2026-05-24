# ── Stage 1: build ──────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw package -DskipTests -B

# ── Stage 2: runtime ─────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

ENV TZ=America/Sao_Paulo
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && \
    echo "America/Sao_Paulo" > /etc/timezone && \
    apk del tzdata

RUN addgroup -S lumyra && adduser -S lumyra -G lumyra
USER lumyra

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
