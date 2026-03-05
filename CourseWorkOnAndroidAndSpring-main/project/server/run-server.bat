@echo off
chcp 65001 >nul
cd /d "%~dp0"

REM Запуск Spring Boot без IntelliJ (если в IDE падает сборка)
set "JAVA_HOME=E:\INST\jdk-17.0.18+8"
call mvnw.cmd spring-boot:run
pause
