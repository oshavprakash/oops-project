@echo off
echo ========================================================
echo Inspecting TiDB Database and Setting Up Default Accounts...
echo ========================================================

set "JAVA_BIN=C:\Program Files\Java\jdk-26.0.2.1\bin"
if exist "src\main\resources\db.properties" (
    copy /Y "src\main\resources\db.properties" "frontend\WEB-INF\classes\db.properties" >nul
)

"%JAVA_BIN%\java.exe" -cp "frontend\WEB-INF\classes;frontend\WEB-INF\lib\*" com.hostelconnect.util.InspectAndSeedDB

pause
