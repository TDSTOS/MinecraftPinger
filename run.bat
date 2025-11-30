@echo off

if not exist "bin\Main.class" (
    echo Compiled classes not found. Running compilation...
    echo.
    call compile.bat
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo Cannot run - compilation failed.
        pause
        exit /b 1
    )
    echo.
)

if not exist "config.properties" (
    echo.
    echo ERROR: config.properties file not found!
    echo.
    echo Please create config.properties with:
    echo   server.ip=your.server.ip
    echo   server.port=25565
    echo.
    pause
    exit /b 1
)

echo Starting Minecraft Player Checker...
echo.
java -cp bin Main
echo.
pause
