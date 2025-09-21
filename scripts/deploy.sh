set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' 

print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

get_container_name() {
    local service_name=$1
    docker-compose ps -q "$service_name" 2>/dev/null | xargs docker inspect --format='{{.Name}}' 2>/dev/null | sed 's/^.//' || echo "$service_name"
}

wait_for_service() {
    local service_name=$1
    local max_attempts=$2
    local attempt=1
    
    print_status "Waiting for $service_name to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if docker-compose ps --services --filter="status=running" | grep -q "^$service_name$"; then
            local container_name=$(get_container_name "$service_name")
            local health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-health-check")
            
            if [ "$health_status" = "healthy" ] || [ "$health_status" = "no-health-check" ]; then
                print_success "$service_name is ready!"
                return 0
            fi
        fi
        
        echo -n "."
        sleep 5
        attempt=$((attempt + 1))
    done
    
    print_error "$service_name failed to become healthy within expected time"
    return 1
}

check_service_logs() {
    local service_name=$1
    print_status "Checking logs for $service_name..."
    
    local logs=$(docker-compose logs --tail=20 "$service_name" 2>/dev/null || echo "")
    
    if echo "$logs" | grep -iE "(error|exception|failed|refused)" | head -3; then
        print_warning "Found potential issues in $service_name logs"
    fi
}

print_status "Deploying E-commerce Analytics Application..."

cd ../Docker

if [ ! -f "docker-compose.yml" ]; then
    print_error "docker-compose.yml not found in current directory"
    exit 1
fi

print_status "Stopping existing containers and cleaning up..."
docker-compose down --remove-orphans --volumes 2>/dev/null || true

print_status "Pulling latest base images..."
docker-compose pull --quiet

print_status "Building application images..."
docker-compose build --parallel --quiet

print_status "Starting infrastructure services (Phase 1: Core Services)..."
docker-compose up -d postgres redis zookeeper

wait_for_service "postgres" 12
wait_for_service "redis" 8
wait_for_service "zookeeper" 20

print_status "Starting infrastructure services (Phase 2: Message Queue)..."
docker-compose up -d kafka

wait_for_service "kafka" 20

print_status "Starting infrastructure services (Phase 3: Monitoring)..."
docker-compose up -d jaeger prometheus grafana

wait_for_service "jaeger" 10
wait_for_service "prometheus" 8
wait_for_service "grafana" 10

print_status "Starting observability services..."
docker-compose up -d otel-collector

print_status "Starting ML Service (must be ready before backend)..."
docker-compose up -d ml-service

print_status "Waiting for ML service to be ready..."
wait_for_service "ml-service" 30

print_status "Starting backend service (depends on ML service)..."
docker-compose up -d backend

print_status "Waiting for backend service to be ready..."
wait_for_service "backend" 60

print_status "Starting frontend and additional services..."
docker-compose up -d frontend kafkaui

wait_for_service "frontend" 10

print_status "Final deployment status check..."
echo ""
echo "=== SERVICE STATUS ==="
docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

echo ""
echo "=== HEALTH CHECK SUMMARY ==="
for service in postgres redis kafka ml-service backend frontend; do
    container_name=$(get_container_name "$service")
    if docker inspect --format='{{.State.Health.Status}}' "$container_name" >/dev/null 2>&1; then
        health=$(docker inspect --format='{{.State.Health.Status}}' "$container_name")
        if [ "$health" = "healthy" ]; then
            print_success "$service: $health"
        else
            print_warning "$service: $health"
            check_service_logs "$service"
        fi
    else
        print_warning "$service: no health check configured"
    fi
done

echo ""
print_success "Deployment complete!"
echo ""
echo " APPLICATION URLS:"
echo "  Frontend:           http://localhost:3000"
echo "  Backend API:        http://localhost:8080"
echo "  ML Service:         http://localhost:8000"
echo ""
echo " MONITORING URLS:"
echo "  Jaeger UI:          http://localhost:16686"
echo "  Prometheus:         http://localhost:9090"
echo "  Grafana:            http://localhost:3001 (admin/admin)"
echo "  Kafka UI:           http://localhost:9095"
echo ""
echo " HEALTH CHECK URLS:"
echo "  Backend Health:     http://localhost:8080/actuator/health"
echo "  ML Service Health:  http://localhost:8000/health"
echo "  OpenTelemetry:      http://localhost:13133"
echo ""
echo " USEFUL COMMANDS:"
echo "  View logs:          docker-compose logs -f [service-name]"
echo "  All logs:           docker-compose logs -f"
echo "  Restart service:    docker-compose restart [service-name]"
echo "  Scale service:      docker-compose up -d --scale [service-name]=N"
echo "  Stop all:           docker-compose down"
echo "  Clean rebuild:      docker-compose down -v && docker-compose build --no-cache"

failed_services=$(docker-compose ps --filter="status=exited" --format="{{.Name}}" | tr '\n' ' ')
if [ ! -z "$failed_services" ]; then
    echo ""
    print_error "Some services failed to start: $failed_services"
    echo "Check logs with: docker-compose logs [service-name]"
    exit 1
else
    echo ""
    print_success "All services started successfully! 🎉"
fi