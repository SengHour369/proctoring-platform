# Generic Dockerfile for every module in this repo — pass the module name as a build arg
# so one file serves eureka-server, config-server, api-gateway, and all nine services.
#
#   docker build --build-arg MODULE=identity-service -t identity-service -f docker/Dockerfile .
#
# docker-compose.yml already wires this up per service; you don't need to run it by hand.
FROM eclipse-temurin:21-jdk AS build
ARG MODULE
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :${MODULE}:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
ARG MODULE
ENV MODULE=${MODULE}
WORKDIR /app
# wget is what docker-compose.prod.yml's healthchecks curl /actuator/health with.
RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*
COPY --from=build /workspace/${MODULE}/build/libs/${MODULE}.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
