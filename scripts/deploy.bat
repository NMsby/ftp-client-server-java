@echo off
setlocal enabledelayedexpansion

REM FTP Server Deployment Script for Windows
title FTP Server Deployment

echo FTP Server Deployment Script
echo ============================
echo.

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
set "DEPLOYMENT_DIR=%PROJECT_DIR%\deployment"
set "JAR_NAME=ftp-server.jar"

REM Check command line arguments
set "ACTION=%1"
if "%ACTION%"=="" set "ACTION=all"

goto :%ACTION% 2>nul || goto :usage

:check
echo [INFO] Checking dependencies...

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found. Please install Java 17 or later.
    exit /b 1
)

for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VERSION=%%i"
    set "JAVA_VERSION=!JAVA_VERSION:"=!"
)
echo [INFO] Java version: %JAVA_VERSION%

REM Check Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found. Please install Maven.
    exit /b 1
)

echo [INFO] Maven found
for /f "tokens=*" %%i in ('mvn -version ^| findstr "Apache Maven"') do echo [INFO] %%i

if "%ACTION%"=="check" goto :end
goto :build

:build
echo [INFO] Building project...

cd /d "%PROJECT_DIR%"

REM Clean and compile
call mvn clean compile
if errorlevel 1 (
    echo [ERROR] Build failed
    exit /b 1
)

REM Run tests
echo [INFO] Running tests...
call mvn test
if errorlevel 1 (
    echo [WARN] Some tests failed, continuing...
)

REM Package
echo [INFO] Creating JAR package...
call mvn package -DskipTests
if errorlevel 1 (
    echo [ERROR] Packaging failed
    exit /b 1
)

REM Find JAR file
for %%f in (target\*.jar) do (
    if not "%%f"=="target\*-sources.jar" (
        if not "%%f"=="target\*-javadoc.jar" (
            set "JAR_FILE=%%f"
        )
    )
)

if not exist "%JAR_FILE%" (
    echo [ERROR] JAR file not found in target directory
    exit /b 1
)

echo [INFO] JAR file created: %JAR_FILE%

if "%ACTION%"=="build" goto :end
goto :prepare

:prepare
echo [INFO] Preparing deployment...

REM Create deployment directory
if not exist "%DEPLOYMENT_DIR%" mkdir "%DEPLOYMENT_DIR%"

REM Copy JAR file
copy "%JAR_FILE%" "%DEPLOYMENT_DIR%\%JAR_NAME%" >nul
echo [INFO] JAR file copied to deployment directory

REM Generate deployment configuration
cd /d "%PROJECT_DIR%"
java -cp "%JAR_FILE%" server.DeploymentManager

echo [INFO] Deployment configuration generated

if "%ACTION%"=="prepare" goto :end
goto :test

:test
echo [INFO] Testing deployment...

cd /d "%DEPLOYMENT_DIR%"

REM Test JAR execution
timeout /t 5 >nul & taskkill /f /im java.exe >nul 2>&1
start /wait /b timeout 10 java -jar "%JAR_NAME%" --help >nul 2>&1

echo [INFO] Deployment test completed

if "%ACTION%"=="test" goto :end
goto :package

:package
echo [INFO] Creating installation package...

set "TIMESTAMP=%date:~-4%%date:~4,2%%date:~7,2%-%time:~0,2%%time:~3,2%%time:~6,2%"
set "TIMESTAMP=%TIMESTAMP: =0%"
set "PACKAGE_NAME=ftp-server-%TIMESTAMP%"
set "PACKAGE_DIR=%PROJECT_DIR%\%PACKAGE_NAME%"

REM Create package directory
mkdir "%PACKAGE_DIR%"

REM Copy deployment files
xcopy "%DEPLOYMENT_DIR%\*" "%PACKAGE_DIR%\" /E /H /Y >nul

REM Copy documentation
if exist "%PROJECT_DIR%\docs" xcopy "%PROJECT_DIR%\docs" "%PACKAGE_DIR%\docs\" /E /H /Y >nul
if exist "%PROJECT_DIR%\README.md" copy "%PROJECT_DIR%\README.md" "%PACKAGE_DIR%\" >nul

REM Create ZIP archive
powershell -command "Compress-Archive -Path '%PACKAGE_DIR%' -DestinationPath '%PROJECT_DIR%\%PACKAGE_NAME%.zip'"

echo [INFO] Installation package created: %PACKAGE_NAME%.zip

REM Cleanup
rmdir /s /q "%PACKAGE_DIR%"

if "%ACTION%"=="package" goto :end
goto :instructions

:instructions
echo.
echo Deployment Instructions
echo ======================
echo.
echo 1. Copy the deployment files to your target server
echo.
echo 2. Configure firewall (see DEPLOYMENT-GUIDE.md)
echo.
echo 3. Start the server:
echo    start-ftp-server.bat
echo.
echo 4. Test connectivity:
echo    test-network.bat [hostname] [port]
echo.
echo [INFO] Deployment files are ready in: %DEPLOYMENT_DIR%
goto :end

:usage
echo Usage: %0 [check^|build^|prepare^|test^|package^|all]
echo   check   - Check dependencies
echo   build   - Build the project
echo   prepare - Prepare deployment files
echo   test    - Test deployment
echo   package - Create installation package
echo   all     - Run all steps (default)
exit /b 1

:end
echo.
echo Deployment script completed.
pause