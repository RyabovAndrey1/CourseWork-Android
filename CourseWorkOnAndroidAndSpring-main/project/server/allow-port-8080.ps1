# Открыть порт 8080 в брандмауэре Windows для доступа с телефона к серверу.
# Запуск: правый клик по файлу -> "Выполнить с помощью PowerShell"
# Или в PowerShell (от администратора): Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass; .\allow-port-8080.ps1

$ruleName = "Student Performance Server 8080"
$existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "Правило '$ruleName' уже есть." -ForegroundColor Yellow
    exit 0
}

New-NetFirewallRule -DisplayName $ruleName -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Private, Domain
Write-Host "Правило добавлено. Телефон (192.168.0.x) может подключаться к этому ПК на порт 8080." -ForegroundColor Green
