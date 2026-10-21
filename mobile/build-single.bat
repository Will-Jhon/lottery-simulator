@echo off
chcp 65001 >nul
setlocal
cd /d %~dp0

where node >nul 2>nul
if errorlevel 1 (
    echo [ERROR] 未找到 Node.js
    echo.
    echo 替代方案：把整个 mobile 文件夹发给朋友，让朋友双击 mobile\serve.bat
    echo （朋友电脑需要有 JDK 8+，然后手机连同一 WiFi 访问打印的地址）
    pause
    exit /b 1
)

node build-single.js
if errorlevel 1 (
    echo.
    echo [ERROR] 打包失败
    pause
    exit /b 1
)

echo.
echo ========================================
echo   打包完成！
echo   文件位置: mobile\dist\双色球模拟器.html
echo   直接发给朋友，浏览器打开即玩
echo ========================================
pause
endlocal
