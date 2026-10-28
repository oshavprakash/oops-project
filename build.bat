@echo off
echo ========================================================
echo Building HostelConnect Web Application...
echo ========================================================

set "JAVA_BIN=C:\Program Files\Java\jdk-26.0.2.1\bin"
set "TOMCAT_LIB=C:\Program Files\Apache Software Foundation\Tomcat 11.0\lib"
set "TOMCAT_WEBAPPS=C:\Program Files\Apache Software Foundation\Tomcat 11.0\webapps"

if not exist "%JAVA_BIN%\javac.exe" (
    echo Java compiler not found at %JAVA_BIN%.
    pause
    exit /b 1
)

if not exist "frontend\WEB-INF\classes" (
    mkdir "frontend\WEB-INF\classes"
)
if not exist "target" (
    mkdir "target"
)

echo [1/4] Compiling Java classes...
dir /s /b src\main\java\*.java > target\sources.txt
"%JAVA_BIN%\javac.exe" -cp "%TOMCAT_LIB%\servlet-api.jar;frontend\WEB-INF\lib\*;frontend\WEB-INF\classes" -d frontend\WEB-INF\classes @target\sources.txt
if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/4] Copying resources...
copy /Y "src\main\resources\*" "frontend\WEB-INF\classes\" >nul

echo [3/4] Creating WAR package (target\hostelconnect.war)...
"%JAVA_BIN%\jar.exe" -cvf target\hostelconnect.war -C frontend . >nul
if %ERRORLEVEL% neq 0 (
    echo Packaging failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [4/4] Deploying to Tomcat 11 webapps...
if exist "%TOMCAT_WEBAPPS%" (
    copy /Y "target\hostelconnect.war" "%TOMCAT_WEBAPPS%\hostelconnect.war" >nul
    echo Deployment successful! WAR copied to %TOMCAT_WEBAPPS%\hostelconnect.war
) else (
    echo Tomcat webapps folder not found at %TOMCAT_WEBAPPS%.
)

echo ========================================================
echo Build and deployment completed successfully!
echo Access your app at: http://localhost:8080/hostelconnect/
echo ========================================================
pause
