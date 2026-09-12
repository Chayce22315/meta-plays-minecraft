@echo off
setlocal
cd /d "%~dp0"
call "%~dp0bot\start-metabot.bat"
exit /b %ERRORLEVEL%
