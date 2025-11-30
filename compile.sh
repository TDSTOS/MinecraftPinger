#!/bin/bash

mkdir -p bin

echo "Compiling Java files..."
javac -d bin src/*.java

if [ $? -eq 0 ]; then
    echo ""
    echo "Compilation successful!"
    echo ""
    echo "To run the application:"
    echo "  ./run.sh"
    echo ""
    echo "Or manually:"
    echo "  java -cp bin Main"
else
    echo ""
    echo "Compilation failed!"
    echo "Please ensure Java JDK is installed and javac is in your PATH."
    exit 1
fi
