@echo off
cd /d "%~dp0"
call gradlew.bat :client:run
if errorlevel 1 pause
