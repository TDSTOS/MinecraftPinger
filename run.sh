#!/bin/bash

if [ ! -f "bin/Main.class" ]; then
    echo "Compiled classes not found. Running compilation..."
    echo ""
    ./compile.sh
    if [ $? -ne 0 ]; then
        echo ""
        echo "Cannot run - compilation failed."
        exit 1
    fi
    echo ""
fi

if [ ! -f "config.properties" ]; then
    echo ""
    echo "ERROR: config.properties file not found!"
    echo ""
    echo "Please create config.properties with:"
    echo "  server.ip=your.server.ip"
    echo "  server.port=25565"
    echo ""
    exit 1
fi

echo "Starting Minecraft Player Checker..."
echo ""
java -cp bin Main
