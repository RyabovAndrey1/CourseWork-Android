@echo off
REM Запустите от имени администратора (ПКМ -> Запуск от имени администратора)
REM Открывает порт 37568 для входящих TCP-подключений (телефон -> сервер на ПК)

netsh advfirewall firewall add rule name="Spring Server 37568" dir=in action=allow protocol=TCP localport=37568
if %errorlevel% equ 0 (
    echo Правило добавлено. Порт 37568 открыт для входящих подключений.
) else (
    echo Ошибка. Запустите файл от имени администратора.
)
pause
