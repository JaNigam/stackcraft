# Api: built with the Maven wrapper, runs on a JRE. Content is baked into the image (content as code, ADR 0002).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY services/api/.mvn .mvn
COPY services/api/mvnw services/api/pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY services/api/src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10002 --create-home api
USER api
WORKDIR /app
COPY --from=build /src/target/stackcraft-api-*.jar app.jar
COPY content /app/content
ENV SPRING_PROFILES_ACTIVE=prod STACKCRAFT_CONTENT_ROOT=/app/content
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=8 CMD wget -qO- http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
