# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar pom.xml primero para aprovechar caché de capas de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copiar fuentes y compilar (sin tests — ya se ejecutan en CI)
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copiar el JAR generado desde el stage de build
COPY --from=build /app/target/ecos-backend-*.jar app.jar

# Puerto que expone la app (Railway lo mapea dinámicamente vía $PORT)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
