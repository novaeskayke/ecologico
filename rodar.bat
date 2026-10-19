@echo off
REM Compila e roda o Ecologico (Windows). Uso: de dois cliques neste arquivo
cd /d "%~dp0"
if not exist out mkdir out
echo Compilando...
javac -encoding UTF-8 -d out -cp "lib/*" src\com\ecologico\*.java
if errorlevel 1 (
  echo Erro ao compilar.
  pause
  exit /b 1
)
echo Iniciando... (para parar: Ctrl + C)
java -cp "out;lib/*" com.ecologico.Main
pause
