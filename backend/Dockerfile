# Multi-stage build: stage 1 compiles the jar with Maven, stage 2 runs it on a slim JRE.
# The final image never contains Maven, the source tree, or the ~/.m2 cache - just the jar - which
# is what keeps it small and is the standard pattern for compiled-language Docker images.

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
# Copy just the pom first so Docker can cache the dependency download layer separately from your
# source code - as long as pom.xml doesn't change, `docker build` reuses this layer instantly.
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
