@echo off
echo Building Unde Server portable application...
echo.
call gradlew.bat :server:createPortable
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Success! Portable app created in: server\build\jpackage\
) else (
    echo.
    echo Build failed. See error above.
)
pause
