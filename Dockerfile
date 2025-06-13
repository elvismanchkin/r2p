# ---- Build Stage ----
    FROM maven:3.9.6-eclipse-temurin-21 AS build
    WORKDIR /app
    COPY pom.xml .
    COPY mvnw .
    COPY .mvn .mvn
    RUN ./mvnw dependency:go-offline
    
    COPY src ./src
    RUN ./mvnw clean package -DskipTests
    
    # ---- Run Stage ----
    FROM eclipse-temurin:21-jre
    WORKDIR /app
    COPY --from=build /app/target/*.jar app.jar
    COPY docker/entrypoint.sh /app/entrypoint.sh
    RUN chmod +x /app/entrypoint.sh && apt-get update && apt-get install -y openssl
    EXPOSE 8080
    ENTRYPOINT ["/app/entrypoint.sh"]