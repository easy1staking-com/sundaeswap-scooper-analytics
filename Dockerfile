# Dockerfile used in compose to build and run project.
FROM openjdk:21-jdk-slim as builder

ADD . /app
WORKDIR /app
RUN ./gradlew bootJar

FROM openjdk:21-jdk-slim

COPY --from=builder /app/build/libs/*.jar /app/app.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "app.jar"]