@echo off
echo Deploying hostelconnect.war to Tomcat...
powershell -ExecutionPolicy Bypass -File "%~dp0deploy.ps1"
echo Done.
pause
