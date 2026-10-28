$src = "D:\oops project\oops-project\target\hostelconnect.war"
$dst = "C:\Program Files\Apache Software Foundation\Tomcat 11.0\webapps\hostelconnect.war"

Write-Host "Copying $src to Tomcat..."
Start-Process powershell -Verb runAs -ArgumentList "-Command & { Copy-Item -LiteralPath '$src' -Destination '$dst' -Force; Write-Host 'Copy completed successfully!'; Start-Sleep -Seconds 2 }" -Wait

# Check if the file is now in Tomcat webapps
$check = Test-Path "C:\Program Files\Apache Software Foundation\Tomcat 11.0\webapps\hostelconnect.war"
if ($check) {
    Write-Host "`n>>> SUCCESS: hostelconnect.war is now deployed in Tomcat webapps! <<<" -ForegroundColor Green
    Write-Host "Wait 5 seconds for Tomcat to unpack it, then refresh: http://localhost:8080/hostelconnect/login1.html"
} else {
    Write-Host "`n>>> NOT COPIED YET: Please approve the Windows administrator popup when it appears. <<<" -ForegroundColor Red
}
