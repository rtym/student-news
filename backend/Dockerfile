FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
# Dependencies first: this layer is only rebuilt when pom.xml changes, so a
# change to Java code reuses it instead of downloading everything again.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/student-news-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
