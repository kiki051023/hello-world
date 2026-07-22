@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set WRAPPER_JAR="%APP_HOME%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    if not exist "%APP_HOME%\.mvn\wrapper" (
        mkdir "%APP_HOME%\.mvn\wrapper"
    )
    powershell -Command "Invoke-WebRequest -Uri '%WRAPPER_URL%' -OutFile '%WRAPPER_JAR%'"
    if %errorlevel% neq 0 (
        echo Failed to download Maven Wrapper
        exit /b 1
    )
)

set JAVA_EXE=java.exe
if defined JAVA_HOME (
    set JAVA_EXE="%JAVA_HOME%\bin\java.exe"
)

"%JAVA_EXE%" -jar "%WRAPPER_JAR%" %*

endlocal
