FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon
CMD ["sh", "-c", "java -Djava.net.preferIPv4Stack=true -jar build/libs/orthoproconnect.jar"]
