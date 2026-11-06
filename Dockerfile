# Etapa de compilación: Maven y JDK 25
FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src src
RUN mvn -B clean package -DskipTests

# Etapa de ejecución: JRE 25
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /app/target/met-coffee-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
