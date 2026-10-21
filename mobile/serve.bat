@echo off
chcp 65001 >nul
setlocal
cd /d %~dp0

echo 正在编译服务器...
javac -encoding UTF-8 Serve.java
if errorlevel 1 (
    echo [ERROR] 编译失败
    pause
    exit /b 1
)

echo.
java -cp . Serve %1
pause
endlocal
