# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Primero solo las dependencias: Docker guarda esta capa en caché y no las vuelve a descargar mientras el pom.xml no cambie.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
# Las pruebas y la cobertura se ejecutan en el CI, no al construir la imagen
RUN ./mvnw package -DskipTests -Djacoco.skip=true -B

# ---------- Etapa 2: ejecutar ----------
# Solo el JRE: imagen más ligera y sin herramientas de compilación
FROM eclipse-temurin:25-jre
WORKDIR /app

# curl solo para el healthcheck; el usuario sin privilegios limita el daño si alguien vulnera la app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 appuser
USER appuser

COPY --from=build --chown=appuser /app/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS "http://localhost:${PORT:-8080}/actuator/health" || exit 1

# MaxRAMPercentage hace que la JVM respete el límite de memoria del contenedor
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]