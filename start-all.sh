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
echo "Subindo infraestrutura Docker (bancos, RabbitMQ, Prometheus, Grafana)..."
# --wait: só continua quando os containers com healthcheck (Postgres, RabbitMQ) estiverem saudáveis.
# Assim os serviços Java não sobem antes de o banco aceitar conexões.
docker compose up -d --wait
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
if [ -d "frontend" ]; then
  if [ ! -d "frontend/node_modules" ]; then
    echo "Instalando dependências do frontend (primeira vez, pode demorar um pouco)..."
    (cd frontend && npm install)
  fi
  echo "Subindo frontend..."
  (cd frontend && nohup npm start > "../logs/frontend.log" 2>&1 &)
  echo $! > ".pids/frontend.pid"
else
  echo "AVISO: pasta frontend não encontrada, pulando."
fi
echo ""
echo "Todos os serviços foram disparados em background."
echo "Logs em: ./logs/<nome-do-servico>.log (frontend em ./logs/frontend.log)"
echo "Acompanhar em tempo real, ex: tail -f logs/order-service.log"
echo ""
echo "Aguarde ~30-40s para os serviços Java subirem e se registrarem no Eureka,"
echo "e ~10-20s para o frontend compilar e ficar disponível."
echo ""
echo "Painéis disponíveis:"
echo "  Frontend:    http://localhost:44200/login"
echo "  Eureka:      http://localhost:48761"
echo "  RabbitMQ:    http://localhost:56721  (guest/guest)"
echo "  Prometheus:  http://localhost:59090"
echo "  Grafana:     http://localhost:53000   (admin/admin)"
echo ""
echo "Swagger UI de cada serviço:"
echo "  Gateway:              http://localhost:48080/swagger-ui.html"
echo "  Customer Service:     http://localhost:48081/swagger-ui.html"
echo "  Product Service:      http://localhost:48082/swagger-ui.html"
echo "  Order Service:        http://localhost:48083/swagger-ui.html"
echo "  Auth Service:         http://localhost:48084/swagger-ui.html"
echo "  Notification Service: http://localhost:48085/swagger-ui.html"