FROM gradle:8.6-jdk17 AS build
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 8080
COPY --from=build /app/build/libs/*.jar /app/app.jar
ENTRYPOINT ["java", "-Xms128m", "-Xmx512m", "-XX:+UseSerialGC", "-jar", "app.jar"]
