@echo off

if not exist "bin" mkdir bin

echo Compiling Java files...
javac -d bin src\*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Compilation successful!
    echo.
    echo To run the application:
    echo   run.bat
    echo.
    echo Or manually:
    echo   java -cp bin Main
) else (
    echo.
    echo Compilation failed!
    echo Please ensure Java JDK is installed and javac is in your PATH.
    echo.
    pause
    exit /b 1
)

pause
