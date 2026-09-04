#!/bin/bash
# stop-all.sh — derruba todos os microsserviços subidos pelo start-all.sh
#
# O plugin do Maven faz "fork" de uma JVM separada para rodar a aplicação,
# então matar só o PID do processo mvn wrapper não é suficiente — o processo
# filho real (com a classe da aplicação) continua vivo. Por isso usamos
# pkill -f casando pelo caminho do projeto, que aparece tanto no processo
# wrapper quanto no processo filho.
#
# Uso:
#   ./stop-all.sh          # para só os microsserviços e o frontend
#   ./stop-all.sh --full   # além disso, também para a infra Docker
#                            (bancos, RabbitMQ, Prometheus, Grafana)
echo "Parando todos os processos do projeto..."
pkill -f distribuidora-atacadista
# Reforço para o frontend: o processo do "ng serve" nem sempre carrega o
# caminho completo do repo no argv (depende de como o npm resolve o binário),
# então o pkill -f acima pode não pegar. Casamos por padrão de comando também.
pkill -f "ng serve" 2>/dev/null || true
sleep 2
echo ""
echo "Processos restantes (deveria estar vazio):"
ps aux | grep -E "distribuidora-atacadista|br\.com\.atlastt" | grep -v grep
echo ""

if [ "$1" == "--full" ]; then
  echo "Parando infraestrutura Docker (bancos, RabbitMQ, Prometheus, Grafana)..."
  docker compose stop
  echo ""
  echo "Feito. Processos e infra Docker parados. Dados dos bancos preservados nos volumes."
  echo "Para religar tudo: ./start-all.sh"
else
  echo "Feito. Bancos e infra Docker continuam rodando."
  echo "Para parar também a infra Docker: ./stop-all.sh --full"
fi