@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo.
echo ========================================
echo   meta plays minecraft - metabot
echo ========================================
echo.

where node >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Node.js was not found on PATH.
  echo Run setup-metabot.bat after installing Node.js.
  pause
  exit /b 1
)

if not exist "node_modules\mineflayer" (
  echo [INFO] Metabot dependencies are not installed yet.
  echo Running setup-metabot.bat...
  call "%~dp0setup-metabot.bat"
  if errorlevel 1 exit /b 1
)

echo.
set "MC_HOST=127.0.0.1"
set "MC_PORT=25565"
set "BOT_USERNAME=MetaBot"
set "CONTROL_PORT=8765"

echo Enter the Minecraft server address.
echo Press Enter to use 127.0.0.1 for a local Minecraft server.
set /p "MC_HOST=Server address [127.0.0.1]: "
if not defined MC_HOST set "MC_HOST=127.0.0.1"

echo.
set /p "MC_PORT=Server port [25565]: "
if not defined MC_PORT set "MC_PORT=25565"

echo.
set /p "BOT_USERNAME=Bot username [MetaBot]: "
if not defined BOT_USERNAME set "BOT_USERNAME=MetaBot"

echo.
echo Starting MetaBot...
echo Server: %MC_HOST%:%MC_PORT%
echo Username: %BOT_USERNAME%
echo Control API: 127.0.0.1:%CONTROL_PORT%
echo.
echo Keep this window open while the bot is playing.
echo Press Ctrl+C to stop it.
echo.

call npm start -- --host "%MC_HOST%" --port "%MC_PORT%" --username "%BOT_USERNAME%" --control-port "%CONTROL_PORT%"

set "EXIT_CODE=%ERRORLEVEL%"
echo.
echo MetaBot stopped with exit code %EXIT_CODE%.
pause
exit /b %EXIT_CODE%
