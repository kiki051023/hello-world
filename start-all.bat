@echo off
REM 学生管理系统一键启动：后端(8080) + 前端(5173)
REM JWT_SECRET 持久化在 %APPDATA%\student-mgmt\jwt_secret.txt，首次运行自动生成

setlocal
set BASEDIR=%~dp0
set SECDIR=%APPDATA%\student-mgmt
set SECFILE=%SECDIR%\jwt_secret.txt

if not exist "%SECDIR%" mkdir "%SECDIR%"
if not exist "%SECFILE%" (
  powershell -Command "[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))" > "%SECFILE%"
  echo [init] JWT_SECRET generated at %SECFILE%
)

REM 清理端口占用：mvnw 退出不等于 java 子进程退出，先杀再启
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080 .*LISTENING"') do (
  echo [cleanup] kill pid %%p on 8080
  taskkill /F /PID %%p >nul 2>&1
)
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":5173 .*LISTENING"') do (
  echo [cleanup] kill pid %%p on 5173
  taskkill /F /PID %%p >nul 2>&1
)

set /p JWT_SECRET=<%SECFILE%

REM DB_PASSWORD 注入：application.yml 已移除默认密码回退，必须显式提供。
REM 优先使用已设置的环境变量，否则默认本地开发密码 123456（README 说明）。
if "%DB_PASSWORD%"=="" set DB_PASSWORD=123456

echo [start] backend :8080 ...
start "student-backend" cmd /c "cd /d %BASEDIR%student-management && set DB_PASSWORD=%DB_PASSWORD% && mvnw.cmd spring-boot:run"

echo [start] frontend :5173 ...
start "student-frontend" cmd /c "cd /d %BASEDIR%student-frontend && npx vite --port 5173"

echo.
echo waiting backend health ...
:wait_backend
timeout /t 2 /nobreak >nul
curl -s -o nul -w "%%{http_code}" http://localhost:8080/api/system/health 2>nul | findstr "200" >nul
if errorlevel 1 goto wait_backend
echo [ok] backend is up
echo [ok] frontend: http://localhost:5173  (admin / 123456)
endlocal
