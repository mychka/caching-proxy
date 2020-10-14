# Required --env system environment variables:
#   backend.addr - backend address, like "https://backend:8080".

# Build project from sources.
FROM gradle:6.6.1-jdk11 AS build

WORKDIR /caching-proxy/project
COPY . .
RUN gradle build


# Build docker image.
FROM openjdk:11

WORKDIR /caching-proxy/project
COPY --from=build /caching-proxy/project/build/libs/caching-proxy-1.0-SNAPSHOT.jar .
EXPOSE 8080 5005

ENTRYPOINT ["java", "-jar", "caching-proxy-1.0-SNAPSHOT.jar"]
