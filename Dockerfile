# ===================================================================
# DOCKERFILE POUR DEPLOIEMENT SPRING BOOT 3 (JAVA 21) SUR RENDER.COM
# ===================================================================

# Étape 1 : Compilation avec Maven
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw
RUN ./mvnw package -DskipTests

# Étape 2 : Exécution de l'application JAR
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
