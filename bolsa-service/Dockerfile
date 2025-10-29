# ===============================
# Stage 1: Build
# ===============================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar primero el JAR de TWS API
COPY lib/TwsApi.jar /tmp/TwsApi.jar

# Instalar TWS API 10.37 en el repositorio Maven local
RUN mvn install:install-file \
    -Dfile=/tmp/TwsApi.jar \
    -DgroupId=com.interactivebrokers \
    -DartifactId=tws-api \
    -Dversion=10.37 \
    -Dpackaging=jar

# Copiar el resto del proyecto
COPY . .

# Compilar el proyecto
RUN mvn clean package -DskipTests

# ===============================
# Stage 2: Run
# ===============================
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","app.jar"]
