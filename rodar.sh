#!/bin/bash
# Compila e roda o Ecologico (Linux / Mac). Uso: ./rodar.sh
cd "$(dirname "$0")"
mkdir -p out
echo "Compilando..."
javac -encoding UTF-8 -d out -cp "lib/*" src/com/ecologico/*.java || { echo "Erro ao compilar."; exit 1; }
echo "Iniciando... (para parar: Ctrl + C)"
java -cp "out:lib/*" com.ecologico.Main
