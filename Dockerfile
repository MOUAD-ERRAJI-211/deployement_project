FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew bootJar -x test
EXPOSE 7860
ENV PORT=7860
CMD ["java", "-jar", "build/libs/orthoproconnect.jar"]