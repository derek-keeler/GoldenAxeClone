@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "GAME_JAR=%SCRIPT_DIR%dist\JavaGoldenAxe.jar"
set "JINPUT_JAR=%SCRIPT_DIR%libs\jinput-2.0.10.jar"
set "JUTILS_JAR=%SCRIPT_DIR%libs\jutils-1.0.0.jar"
set "NATIVES_DIR=%SCRIPT_DIR%libs\natives"

if not exist "%GAME_JAR%" (
  echo [ERROR] Game jar not found: "%GAME_JAR%"
  echo Build first with: ant -noinput -buildfile JavaGoldenAxe\build.xml clean jar
  exit /b 1
)

if not exist "%JINPUT_JAR%" (
  echo [ERROR] Missing dependency: "%JINPUT_JAR%"
  echo Download it from Maven Central:
  echo   https://repo1.maven.org/maven2/net/java/jinput/jinput/2.0.10/jinput-2.0.10.jar
  exit /b 1
)

if not exist "%JUTILS_JAR%" (
  echo [ERROR] Missing dependency: "%JUTILS_JAR%"
  echo Download it from Maven Central:
  echo   https://repo1.maven.org/maven2/net/java/jutils/jutils/1.0.0/jutils-1.0.0.jar
  exit /b 1
)

if not exist "%NATIVES_DIR%\jinput-dx8_64.dll" (
  echo [ERROR] Missing native library: "%NATIVES_DIR%\jinput-dx8_64.dll"
  echo Extract from:
  echo   https://repo1.maven.org/maven2/net/java/jinput/jinput/2.0.10/jinput-2.0.10-natives-all.jar
  exit /b 1
)

if not exist "%NATIVES_DIR%\jinput-raw_64.dll" (
  echo [ERROR] Missing native library: "%NATIVES_DIR%\jinput-raw_64.dll"
  echo Extract from:
  echo   https://repo1.maven.org/maven2/net/java/jinput/jinput/2.0.10/jinput-2.0.10-natives-all.jar
  exit /b 1
)

java -Dnet.java.games.input.librarypath="%NATIVES_DIR%" -cp "%JINPUT_JAR%;%JUTILS_JAR%;%GAME_JAR%" main.Main
