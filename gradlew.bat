@echo off
setlocal
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
set "VERSION=8.10.2"
set "CACHE=%USERPROFILE%\.gradle\wrapper\dists\gradle-%VERSION%-bin"
set "DIST=%CACHE%\gradle-%VERSION%"
if not exist "%DIST%\bin\gradle.bat" (
  if not exist "%CACHE%" mkdir "%CACHE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing https://services.gradle.org/distributions/gradle-%VERSION%-bin.zip -OutFile '%CACHE%\gradle.zip'; Expand-Archive -Force '%CACHE%\gradle.zip' '%CACHE%'; Remove-Item '%CACHE%\gradle.zip'"
)
call "%DIST%\bin\gradle.bat" %*
endlocal
