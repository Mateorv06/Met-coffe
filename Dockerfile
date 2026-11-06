# Etapa de compilación
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
# Instalar Maven en la imagen alpina ligera
RUN apk add --no-cache maven
# Copiar el pom.xml y descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Copiar el código fuente y empaquetar la app omitiendo pruebas
COPY src src
RUN mvn package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copiar el archivo .jar generado en la etapa anterior
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
