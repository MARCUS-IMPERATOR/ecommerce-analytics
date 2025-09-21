FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache curl

WORKDIR /app

COPY target/*.jar app.jar

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]