# Исправление ошибки jlink.exe (exit -1073741819)

При сборке Android-проекта возникает:

```
Error while executing process ... jlink.exe with arguments {--version}
Process finished with non-zero exit value -1073741819
```

Код **-1073741819** (0xC0000005) — это **ACCESS_VIOLATION** в Windows: процесс `jlink.exe` падает на вашем ПК. Так может происходить и с JDK из Android Studio (JBR), и с отдельно установленной JDK (например, `E:\INST\jdk-17.0.18+8`).

## Что сделать по шагам

### 1. Установить Visual C++ Redistributable (чаще всего этого достаточно)

На Windows часто не хватает библиотек времени выполнения Visual C++ (в т.ч. `vcruntime140_1.dll`), из‑за чего падают `jlink` и другие нативные утилиты JDK.

1. Скачайте и установите:
   - **64-bit:** https://aka.ms/vs/17/release/vc_redist.x64.exe  
   - **32-bit (если собираете под x86):** https://aka.ms/vs/17/release/vc_redist.x86.exe  
2. Перезагрузите компьютер.  
3. Проверьте в командной строке:
   ```cmd
   "E:\INST\jdk-17.0.18+8\bin\jlink.exe" --version
   ```
   Должна вывестись версия jlink без падения. После этого снова запустите сборку (Build → Rebuild Project или `gradlew assembleDebug`).

### 2. Проверить антивирус и исключения

Временно отключите антивирус или добавьте в исключения папку JDK, например:

- `E:\INST\jdk-17.0.18+8\bin`

Затем снова выполните в cmd:

```cmd
"E:\INST\jdk-17.0.18+8\bin\jlink.exe" --version
```

Если после этого команда отрабатывает, настройте постоянные исключения для этой папки.

### 3. Убедиться, что Gradle использует нужную JDK

В **gradle.properties** (в корне проекта `mobile`) можно явно указать JDK:

```properties
org.gradle.java.home=E:/INST/jdk-17.0.18+8
```

В **Android Studio**: **File → Settings → Build, Execution, Deployment → Build Tools → Gradle** — в поле **Gradle JDK** выберите ту же JDK (например, `E:\INST\jdk-17.0.18+8`).

После смены JDK остановите демон Gradle и соберите проект заново:

```cmd
cd E:\INST\CourseWorkRyabov\CourseWorkOnAndroidAndSpring-main\project\mobile
gradlew --stop
gradlew assembleDebug
```

### 4. Очистить кэш трансформов Gradle

Если после установки VC++ Redistributable или смены JDK ошибка всё ещё есть, удалите кэш трансформ и соберите снова:

```cmd
rd /s /q "%USERPROFILE%\.gradle\caches\transforms"
gradlew clean assembleDebug
```

---

Итог: в большинстве случаев достаточно **установить VC++ Redistributable и перезагрузить ПК**, после чего `jlink --version` и сборка начинают работать.
