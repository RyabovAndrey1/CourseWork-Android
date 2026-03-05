@echo off
chcp 65001 >nul
cd /d "%~dp0"

REM Запуск сервера на JBR из IntelliJ (обход падения -1073741819 с внешней JDK)
set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.2\jbr"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo JBR не найден: %JAVA_HOME%
    echo Укажите путь к jbr вручную в этом .bat или установите VC++ Redistributable.
    pause
    exit /b 1
)
call mvnw.cmd spring-boot:run
pause
