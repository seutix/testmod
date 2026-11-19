@echo off
setlocal
chcp 65001 >nul
title Fapcraft 1.12.2 - Build

cd /d "%~dp0"
echo ==========================================
echo       FAPCRAFT 1.12.2 - BUILD
echo ==========================================
echo.
echo [1/3] Checking Java...
where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java is not installed or is not in PATH.
  echo Install JDK 21, then run this file again.
  goto failed
)
java -version
echo.
echo [2/3] Checking Maven...
where mvn >nul 2>nul
if errorlevel 1 (
  echo ERROR: Maven is not installed or is not in PATH.
  echo Install Apache Maven and add its bin folder to PATH.
  goto failed
)
mvn -version
echo.
echo [3/3] Building the Forge 1.12.2 mod...
call mvn -B clean package
if errorlevel 1 goto failed

if not exist "target\fapcraft-1.12.2-1.1.3.jar" (
  echo ERROR: Expected reobfuscated jar was not created:
  echo target\fapcraft-1.12.2-1.1.3.jar
  echo Check the Maven output above.
  goto failed
)

if not exist "dist" mkdir "dist"
copy /Y "target\fapcraft-1.12.2-1.1.3.jar" "dist\fapcraft-1.12.2-1.1.3.jar" >nul
if errorlevel 1 goto failed

echo.
echo ==========================================
echo BUILD SUCCESSFUL!
echo Your Forge mod is here:
echo %CD%\dist\fapcraft-1.12.2-1.1.3.jar
echo ==========================================
echo.
echo Copy this JAR into your Minecraft 1.12.2
echo Forge instance mods folder.
echo.
pause
exit /b 0

:failed
echo.
echo ==========================================
echo BUILD FAILED - read the error above.
echo ==========================================
echo.
pause
exit /b 1
