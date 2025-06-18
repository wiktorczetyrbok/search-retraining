FROM maven:3.9.6-eclipse-temurin-21 AS builder
LABEL authors="wiktorczetyrbok"

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY --from=builder /app/target/bookstore-0.1.jar app.jar

ENV MICRONAUT_ENVIRONMENTS=cloud

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
