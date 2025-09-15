FROM eclipse-temurin:21-jdk-alpine

# Install curl (for downloading agent if needed)
RUN apk add --no-cache curl

WORKDIR /app

# Copy Spring Boot fat JAR
COPY target/*.jar app.jar

# Download OpenTelemetry Java agent directly into the container
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

EXPOSE 8080

# Use JAVA_OPTS from docker-compose + add defaults
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]