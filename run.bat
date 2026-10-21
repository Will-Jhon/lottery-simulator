@echo off
chcp 65001 >nul
setlocal
cd /d %~dp0

if not exist out mkdir out
dir /s /b src\*.java > sources.txt 2>nul
if errorlevel 1 (
    echo [ERROR] 未找到 Java 源文件
    pause
    exit /b 1
)

echo 正在编译...
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo.
    echo [ERROR] 编译失败，请检查上方错误信息
    pause
    exit /b 1
)

echo 启动 本地双色球模拟器 ...
java -Dfile.encoding=UTF-8 -cp out lottery.Main
if errorlevel 1 pause
endlocal
