FROM gradle:8.10.2-jdk21 AS build
WORKDIR /src
COPY . .
RUN chmod +x ./gradlew && ./gradlew :my-bank-notification-app:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /src/my-bank-notification-app/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]