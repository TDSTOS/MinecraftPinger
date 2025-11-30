@echo off
echo Minecraft Player Checker - Update Script
echo ==========================================
echo.
echo Waiting for application to shut down...
timeout /t 3 /nobreak >nul
echo.
echo Extracting update...
powershell -Command "Expand-Archive -Path 'updates\latest.zip' -DestinationPath '.' -Force"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to extract update!
    pause
    exit /b 1
)
echo.
echo Update extracted successfully!
echo.
echo Restarting application...
timeout /t 2 /nobreak >nul
start "Minecraft Player Checker" java -jar Main.jar
exit
