# ---- Stage 1: Build ----
FROM eclipse-temurin:23-jdk AS builder
LABEL authors="wiktorczetyrbok"

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:23-jdk

WORKDIR /app

COPY --from=builder /app/target/clickstream-app-1.0.jar app.jar

ENV SPRING_PROFILES_ACTIVE=cloud

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
