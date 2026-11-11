# Build and runtime use the same Java major version as the project target
# (java.version in pom.xml) and the CI unit-test job: 21, an LTS release.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Dependencies first: this layer is only rebuilt when pom.xml changes, so a
# change to Java code reuses it instead of downloading everything again.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# JRE only, on Alpine: no compiler, no Maven, no Ubuntu userland in the image
# that actually runs.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/student-news-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
