# ── Build stage ───────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY plugin-api/pom.xml  plugin-api/
COPY core/pom.xml        core/
COPY plugin-bundle/pom.xml plugin-bundle/

# Resolve dependencies before copying source (improves layer caching)
RUN mvn dependency:go-offline -q --fail-at-end || true

COPY plugin-api/src  plugin-api/src
COPY core/src        core/src
COPY plugin-bundle/src plugin-bundle/src

RUN mvn package -DskipTests -pl core -am

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /workspace/core/target/core-*.jar app.jar

# Directories are created empty; both are expected to be mounted as volumes
RUN mkdir -p plugins input

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
