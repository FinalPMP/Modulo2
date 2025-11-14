FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .

RUN mvn -q -DskipTests -pl gateway -am package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/gateway/target/*.jar app.jar

EXPOSE 9100
ENTRYPOINT ["java", "-jar", "app.jar"]
