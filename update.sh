#!/bin/bash
echo "Minecraft Player Checker - Update Script"
echo "=========================================="
echo ""
echo "Waiting for application to shut down..."
sleep 3
echo ""
echo "Extracting update..."
unzip -o updates/latest.zip -d .
if [ $? -ne 0 ]; then
    echo "ERROR: Failed to extract update!"
    exit 1
fi
echo ""
echo "Update extracted successfully!"
echo ""
echo "Restarting application..."
sleep 2
nohup java -jar Main.jar > /dev/null 2>&1 &
echo "Application restarted in background"
exit 0
