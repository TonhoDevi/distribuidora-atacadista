#!/bin/bash
# start-all.sh — sobe todos os microsserviços em background, cada um com seu log próprio.
# Rodar a partir da raiz do monorepo: ./start-all.sh

set -e

# Carrega variáveis de ambiente do .env (se existir), sem sobrescrever
# variáveis já exportadas manualmente no shell.
if [ -f .env ]; then
  echo "Carregando variáveis de .env..."
  set -a
  source .env
  set +a
fi

mkdir -p logs
mkdir -p .pids

SERVICES=(
  "eureka-server"
  "customer-service"
  "product-service"
  "order-service"
  "auth-service"
  "gateway-service"
  "notification-service"
)

echo "Subindo bancos Docker (se ainda não estiverem no ar)..."
docker start pg-customer pg-product pg-order pg-auth 2>/dev/null || true

echo "Subindo RabbitMQ (se ainda não estiver no ar)..."
docker start rabbitmq 2>/dev/null || \
  docker run --name rabbitmq \
    -p 5672:5672 \
    -p 15672:15672 \
    -d rabbitmq:3-management

echo "Subindo Prometheus (se ainda não estiver no ar)..."
docker start prometheus 2>/dev/null || \
  docker run --name prometheus \
    -p 9090:9090 \
    --add-host=host.docker.internal:host-gateway \
    -v "$(pwd)/prometheus.yml:/etc/prometheus/prometheus.yml" \
    -d prom/prometheus

echo "Subindo Grafana (se ainda não estiver no ar)..."
docker start grafana 2>/dev/null || \
  docker run --name grafana \
    -p 3000:3000 \
    --add-host=host.docker.internal:host-gateway \
    -d grafana/grafana

for service in "${SERVICES[@]}"; do
  if [ -d "$service" ]; then
    echo "Subindo $service..."
    (cd "$service" && nohup mvn spring-boot:run > "../logs/$service.log" 2>&1 &)
    echo $! > ".pids/$service.pid"
    sleep 2
  else
    echo "AVISO: pasta $service não encontrada, pulando."
  fi
done

echo ""
echo "Todos os serviços foram disparados em background."
echo "Logs em: ./logs/<nome-do-servico>.log"
echo "Acompanhar em tempo real, ex: tail -f logs/order-service.log"
echo ""
echo "Aguarde ~30-40s para todos subirem e se registrarem no Eureka."
echo ""
echo "Painéis disponíveis:"
echo "  Eureka:      http://localhost:8761"
echo "  RabbitMQ:    http://localhost:15672  (guest/guest)"
echo "  Prometheus:  http://localhost:9090"
echo "  Grafana:     http://localhost:3000   (admin/admin)"