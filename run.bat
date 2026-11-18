@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
echo Compiling NutriTrack...
javac -encoding UTF-8 -d bin -cp "lib\mysql-connector-j.jar;src" src\Main.java src\database\*.java src\exception\*.java src\model\*.java src\util\*.java src\dao\*.java src\dao\impl\*.java src\service\*.java src\gui\*.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)
echo Launching NutriTrack...
java -cp "bin;lib\mysql-connector-j.jar" Main
