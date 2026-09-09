@echo off
setlocal
cd /d "%~dp0"
call "%~dp0bot\setup-metabot.bat"
exit /b %ERRORLEVEL%
