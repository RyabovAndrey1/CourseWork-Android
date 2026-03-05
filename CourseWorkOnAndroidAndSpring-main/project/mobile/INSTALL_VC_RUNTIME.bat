@echo off
chcp 65001 >nul
echo ============================================
echo   AAPT2 / jlink / jmod -1073741819 на Windows
echo   (Visual C++ Redistributable x64)
echo ============================================
echo.
echo ВАЖНО: Запускайте этот файл ОТ ИМЕНИ АДМИНИСТРАТОРА
echo        (ПКМ по файлу - "Запуск от имени администратора").
echo.

set "URL=https://aka.ms/vs/17/release/vc_redist.x64.exe"
set "TEMPEXE=%TEMP%\vc_redist.x64.exe"

echo Скачиваю Visual C++ 2015-2022 Redistributable (x64)...
powershell -NoProfile -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%URL%' -OutFile '%TEMPEXE%' -UseBasicParsing }"
if %errorlevel% neq 0 (
    echo Не удалось скачать. Открываю страницу загрузки в браузере.
    start "" "https://aka.ms/vs/17/release/vc_redist.x64.exe"
    echo.
    echo Скачайте, установите (или "Исправить", если уже стоял), ПЕРЕЗАГРУЗИТЕ ПК, затем Rebuild Project.
    pause
    exit /b 1
)

echo Запускаю установщик...
echo Если VC++ уже установлен - выберите "Исправить" (Repair).
echo.
start /wait "" "%TEMPEXE%"

echo.
echo Обязательно ПЕРЕЗАГРУЗИТЕ компьютер - иначе jlink/jmod могут продолжать падать.
echo После перезагрузки: Android Studio - Build - Rebuild Project.
pause
