# ============================================================================
# Imagen de producción del backend ParDrops (Spring Boot 4 + Java 25)
# ----------------------------------------------------------------------------
# Construcción en dos etapas: la primera compila el JAR con Maven y la segunda
# solo contiene el JRE y el artefacto, para reducir tamaño y superficie de
# ataque. Compatible con el plan gratuito de Render.
# ============================================================================

# ---------------------------------------------------------------------------
# Etapa 1: compilación
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

# Primero las dependencias: se cachean mientras no cambie el pom.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Después el código y el empaquetado (sin tests: se ejecutan en CI).
COPY src/ src/
RUN mvn -B -q -DskipTests package \
    && cp target/pardrops-*.jar /workspace/app.jar

# ---------------------------------------------------------------------------
# Etapa 2: ejecución
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre

# Usuario sin privilegios.
RUN groupadd --system pardrops && useradd --system --gid pardrops --home /app pardrops

WORKDIR /app
COPY --from=build /workspace/app.jar /app/app.jar

# Carpeta de datos (outbox de correo y almacén local de respaldo).
RUN mkdir -p /app/data && chown -R pardrops:pardrops /app

USER pardrops

# Render inyecta PORT; el perfil prod activa la configuración de producción.
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
