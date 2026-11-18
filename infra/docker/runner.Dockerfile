# Runner: needs a full JDK at runtime (javac + java for the child JVM). Build context: repo root.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY services/runner/.mvn .mvn
COPY services/runner/mvnw services/runner/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY services/runner/src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jdk
# Unprivileged user: snippets run as this user, never as root.
RUN useradd --system --uid 10001 --create-home runner
USER runner
WORKDIR /app
COPY --from=build /src/target/stackcraft-runner-*.jar app.jar
ENV RUNNER_BIND=0.0.0.0 JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50"
EXPOSE 8091
HEALTHCHECK --interval=15s --timeout=3s --retries=5 CMD wget -qO- http://127.0.0.1:8091/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
