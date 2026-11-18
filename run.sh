#!/bin/bash
# Automatically navigate to whichever folder NutriTrack is placed in on macOS
cd "$(dirname "$0")"

mkdir -p bin

echo "Compiling NutriTrack on macOS..."
javac -encoding UTF-8 -d bin -cp "lib/mysql-connector-j.jar:src" src/Main.java src/database/*.java src/exception/*.java src/model/*.java src/util/*.java src/dao/*.java src/dao/impl/*.java src/service/*.java src/gui/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Launching NutriTrack..."
java -cp "bin:lib/mysql-connector-j.jar" Main
