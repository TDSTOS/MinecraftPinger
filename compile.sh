#!/bin/bash

echo "Compiling Java files..."
javac -d bin src/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    echo ""
    echo "To run the application:"
    echo "  ./run.sh"
    echo ""
    echo "Or manually:"
    echo "  java -cp bin Main"
else
    echo "Compilation failed!"
    exit 1
fi
