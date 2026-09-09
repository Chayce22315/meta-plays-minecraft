@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo ========================================
echo   meta plays minecraft - metabot setup
echo ========================================
echo.

where node >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Node.js was not found on PATH.
  echo Install Node.js, restart your terminal, and run this script again.
  exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
  echo [ERROR] npm was not found on PATH.
  echo Reinstall Node.js with npm enabled, then run this script again.
  exit /b 1
)

echo [OK] Node.js:
node --version
echo [OK] npm:
npm --version
echo.

echo Installing metabot dependencies...
npm install
if errorlevel 1 (
  echo.
  echo [ERROR] npm install failed.
  exit /b 1
)

echo.
echo ========================================
echo   setup complete
echo ========================================
echo.
echo You can now run start-metabot.bat from this folder.
echo.
pause
endlocal
