# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the Spring Boot jar ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Dependencies in their own layer: Docker reuses it while pom.xml does not change.
RUN ./mvnw -B -q dependency:go-offline
COPY src/ src/
# Tests run in CI (GitHub Actions); here we only package. The extracted layout starts faster
# than the fat jar.
RUN ./mvnw -B -q package -DskipTests \
    && java -Djarmode=tools -jar target/arrima.jar extract --destination extracted

# ---- Runtime stage: JRE only, non-root user ----
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 10001 --no-create-home arrima
WORKDIR /app
COPY --from=build /workspace/extracted/ ./

# JVM options, read by the java launcher both in the training run below and at start-up (the AOT
# cache only works if they are the same). Tuned for Render's free instance (512 MB RAM, 0.1 CPU):
#  - SerialGC: the lightest collector, with no parallel GC threads competing for the CPU.
#  - MaxRAMPercentage=60: about 300 MB of heap, leaving room for metaspace, threads and code cache.
#  - UseCompactObjectHeaders (Java 25): 8-byte object headers instead of 12, so less heap.
#  - TieredStopAtLevel=1: only the quick C1 compiler; faster start-up and less CPU spent on JIT.
ENV JDK_JAVA_OPTIONS="-XX:+UseSerialGC -XX:MaxRAMPercentage=60 -XX:+UseCompactObjectHeaders -XX:TieredStopAtLevel=1 -Xss512k"

# Training run for Java 25's AOT cache (JEP 483/514/515). The application starts once here, without
# a database (context only, no Flyway, no connection), and the JVM saves the classes it loaded and
# linked into app.aot. Real start-ups then skip that work, which on 0.1 CPU is most of the time.
# The throwaway JWT_SECRET only lives during this command. If the cache ever doesn't match the JVM,
# the JVM ignores it and simply starts the slow way.
RUN JWT_SECRET="$(head -c 32 /dev/urandom | base64)" \
    java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar arrima.jar \
        --spring.flyway.enabled=false \
        --spring.jpa.hibernate.ddl-auto=none \
        --spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
        --spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
        --spring.datasource.url=jdbc:postgresql://localhost:5432/training \
        --spring.datasource.username=training \
        --spring.datasource.password=training \
        --arrima.storage.provider=local \
        --arrima.mail.provider=log \
        --arrima.mail.app-url=http://localhost

USER arrima
EXPOSE 8080
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-jar", "arrima.jar"]
