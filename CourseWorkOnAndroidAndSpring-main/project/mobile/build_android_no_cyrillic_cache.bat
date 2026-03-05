@echo off
REM Сборка Android без кэша в пути с кириллицей (устраняет падение jlink -1073741819).
REM Задаёт GRADLE_USER_HOME в папке проекта (E:\... или текущий диск без кириллицы).

set "SCRIPT_DIR=%~dp0"
set "GRADLE_USER_HOME=%SCRIPT_DIR%gradle-user-home"
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"
echo Using GRADLE_USER_HOME=%GRADLE_USER_HOME%

call "%SCRIPT_DIR%gradlew.bat" %*
exit /b %ERRORLEVEL%
