#!/bin/bash
# stop-all.sh — derruba todos os microsserviços subidos pelo start-all.sh
#
# O plugin do Maven faz "fork" de uma JVM separada para rodar a aplicação,
# então matar só o PID do processo mvn wrapper não é suficiente — o processo
# filho real (com a classe da aplicação) continua vivo. Por isso usamos
# pkill -f casando pelo caminho do projeto, que aparece tanto no processo
# wrapper quanto no processo filho.

echo "Parando todos os processos do projeto..."
pkill -f distribuidora-atacadista

sleep 2
echo ""
echo "Processos restantes (deveria estar vazio):"
ps aux | grep -E "distribuidora-atacadista|br\.com\.atlastt" | grep -v grep

echo ""
echo "Feito. Bancos Docker continuam rodando (docker stop pg-customer pg-product pg-order pg-auth se quiser parar também)."