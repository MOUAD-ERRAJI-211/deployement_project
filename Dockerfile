FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon
EXPOSE 8081
CMD ["java", "-jar", "build/libs/orthoproconnect.jar", "--server.port=8081"]
