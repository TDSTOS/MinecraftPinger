@echo off
echo Compiling Java files...
javac -d bin src\*.java

if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
    echo.
    echo To run the application:
    echo   run.bat
    echo.
    echo Or manually:
    echo   java -cp bin Main
) else (
    echo Compilation failed!
    exit /b 1
)
