# Stage 1: build the application with Maven and the full JDK.
FROM maven:3.10-eclipse-temurin-27 AS build

# Use /app as the working directory for the build.
WORKDIR /app

# Copy only pom.xml first so dependencies can be cached separately.
COPY pom.xml .

# Download Maven dependencies before copying the source code.
RUN mvn -B -q dependency:go-offline

# Copy the application source code.
COPY src ./src

# Build the executable JAR without running tests.
RUN mvn -B -q clean package -DskipTests


# Stage 2: run the application using only a minimal JRE.
FROM eclipse-temurin:27-jre-alpine

# Use /app as the runtime working directory.
WORKDIR /app

# Create a non-root user and prepare its writable upload directory.
RUN addgroup -S -g 10001 app \
    && adduser -S -u 10001 -G app app \
    && mkdir -p /data/avatars \
    && chown -R app:app /data/avatars

# Copy only the built JAR from the build stage.
COPY --from=build /app/target/avatar-service.jar /app/app.jar

# Document the port used by avatar-service.
EXPOSE 8090

# Run the service as the non-root user.
USER app

# Start the Spring Boot application.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
