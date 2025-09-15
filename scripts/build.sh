set -e

echo "Building E-commerce Analytics Application..."

if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Start Docker first."
    exit 1
fi

echo "Setting up OpenTelemetry monitoring..."
cd ../Backend/ecomAnalytics/
if [ ! -f "opentelemetry-javaagent.jar" ]; then
    echo "⬇Downloading OpenTelemetry Java agent..."
    curl -L -o opentelemetry-javaagent.jar \
        "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar"
fi

echo "Building Spring Boot application..."
if command -v mvn &> /dev/null; then
    mvn clean package -DskipTests
else
    echo "Maven not found. Install Maven or build manually."
    exit 1
fi

cd ../../Docker

echo "Building Docker images..."
docker-compose build --no-cache --parallel

echo "Build complete!"
