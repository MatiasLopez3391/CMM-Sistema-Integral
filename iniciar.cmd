@echo off
setlocal
chcp 65001 >nul
title Sistema Integral CMM
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 (
  echo.
  echo [ERROR] No se encontro Java en este equipo.
  echo Instale Java 21 ^(por ejemplo Eclipse Temurin 21: https://adoptium.net/temurin/releases/?version=21^)
  echo marcando la opcion "Set JAVA_HOME" y "Add to PATH", cierre esta ventana y vuelva a ejecutar iniciar.cmd
  echo.
  pause
  exit /b 1
)

for /f tokens^=2^ delims^=^" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%v
for /f "tokens=1 delims=." %%m in ("%JAVA_VER%") do set JAVA_MAJOR=%%m
if %JAVA_MAJOR% LSS 17 (
  echo.
  echo [ERROR] Se encontro Java %JAVA_VER%, pero el sistema necesita Java 17 o superior.
  echo Descargue Java 21: https://adoptium.net/temurin/releases/?version=21
  echo.
  pause
  exit /b 1
)

echo Java %JAVA_VER% encontrado.
echo Compilando y luego iniciando el sistema ^(la primera vez descarga Maven y las librerias, puede tardar unos minutos^)...
echo Cuando aparezca "Sistema listo" abra http://localhost:8080
echo.
call "%~dp0mvnw.cmd" -q -DskipTests package
if errorlevel 1 (
  echo [ERROR] No se pudo compilar el proyecto.
  pause
  exit /b 1
)
java -jar "%~dp0target\cmm-sistema-integral-0.2.0-SNAPSHOT.jar"
pause
