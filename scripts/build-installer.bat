@echo off
echo Building Unde Server installer...
echo.
echo Note: WiX Toolset must be installed and in PATH
echo Download from: https://wixtoolset.org
echo.
call gradlew.bat :server:createPackage
if %ERRORLEVEL% EQU 0 (
    echo.
    echo Success! Installer created in: server\build\jpackage\
) else (
    echo.
    echo Build failed. See error above.
)
pause
