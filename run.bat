@echo off

if not exist "bin\" (
    echo Compiled classes not found. Running compilation...
    call compile.bat
    echo.
)

echo Starting Minecraft Player Checker...
echo.
java -cp bin Main
