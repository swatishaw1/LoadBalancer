
# Build either existing Spring Boot project
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

ARG PROJECT_DIR
ARG ARTIFACT_ID

COPY . .

RUN mvn -f "${PROJECT_DIR}/pom.xml" -DskipTests package \
    && cp "${PROJECT_DIR}/target/${ARTIFACT_ID}-0.0.1-SNAPSHOT.jar" /workspace/app.jar

# Runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
