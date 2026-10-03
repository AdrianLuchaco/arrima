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
USER arrima
EXPOSE 8080
# Tuned for Render's free instance (512 MB RAM, 0.1 CPU):
#  - SerialGC: the lightest collector, with no parallel GC threads competing for the CPU.
#  - MaxRAMPercentage=60: about 300 MB of heap, leaving room for metaspace, threads and code cache.
#  - UseCompactObjectHeaders (Java 25): 8-byte object headers instead of 12, so less heap.
#  - TieredStopAtLevel=1: only the quick C1 compiler; faster start-up and less CPU spent on JIT.
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=60", "-XX:+UseCompactObjectHeaders", \
            "-XX:TieredStopAtLevel=1", "-Xss512k", "-jar", "arrima.jar"]
