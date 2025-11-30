#!/bin/bash

if [ ! -d "bin" ]; then
    echo "Compiled classes not found. Running compilation..."
    ./compile.sh
    echo ""
fi

echo "Starting Minecraft Player Checker..."
echo ""
java -cp bin Main
