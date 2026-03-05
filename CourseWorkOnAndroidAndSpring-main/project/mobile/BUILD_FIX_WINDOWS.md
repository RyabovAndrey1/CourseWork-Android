# Ошибки сборки на Windows

## СРОЧНО: AAPT2 Daemon startup failed / Universal C Runtime

Если сборка падает с **"AAPT2 ... Daemon startup failed — Please check if you installed the Windows Universal C Runtime"**:

1. **Запустите от имени администратора** файл в этой папке: **`INSTALL_VC_RUNTIME.bat`**  
   (ПКМ по файлу → «Запуск от имени администратора»). Он скачает и установит нужные компоненты.
2. **Перезагрузите компьютер.**
3. В Android Studio: **Build → Rebuild Project.**

Если скрипт не сработал — скачайте и установите вручную: https://aka.ms/vs/17/release/vc_redist.x64.exe затем перезагрузка и снова сборка.

---

## 1. jmod.exe / jlink.exe — exit -1073741819 (JdkImageTransform)

Ошибка **"Error while executing process ... jlink.exe"** (или **jmod.exe**) с **exit value -1073741819** при сборке — падение нативных утилит JDK на Windows. Код -1073741819 (ACCESS_VIOLATION) обычно связан с отсутствием или несовместимой версией **Visual C++ Redistributable**.

### Шаг 1 (обязательно): VC++ Redistributable и перезагрузка

1. **Запустите от имени администратора** скрипт **`INSTALL_VC_RUNTIME.bat`** в этой папке (ПКМ → «Запуск от имени администратора»).  
   Или вручную: скачайте и установите [Visual C++ 2015–2022 Redistributable (x64)](https://aka.ms/vs/17/release/vc_redist.x64.exe). Если компонент уже установлен — выберите **«Исправить» (Repair)**.
2. **Обязательно перезагрузите компьютер** — без перезагрузки падение jlink/jmod часто сохраняется.
3. После перезагрузки снова выполните **Build → Rebuild Project**.

### Шаг 2: Gradle JDK = JDK 17

Если ошибка была при использовании **JBR** (путь к процессу из папки `jbr`), переведите Gradle на JDK 17:

1. **File → Settings** → **Build, Execution, Deployment → Build Tools → Gradle**.
2. **Gradle JDK** → выберите **JDK 17** (или добавьте путь, например **E:\INST\jdk-17.0.18+8**). **Apply** → **OK**.
3. **File → Invalidate Caches... → Invalidate and Restart**, затем **Build → Clean Project** и **Build → Rebuild Project**.

### Если jlink падает уже с JDK 17 (E:\INST\jdk-17.0.18+8)

Значит, на этой системе падают нативные бинарники вашего дистрибутива JDK. Сделайте по порядку:

1. **Повторите шаг 1** (VC++ x64, при необходимости «Исправить», затем **перезагрузка**).
2. **Проверка вручную:** откройте cmd и выполните:
   ```bat
   E:\INST\jdk-17.0.18+8\bin\jlink.exe --version
   ```
   Если здесь тоже падение или ошибка — проблема в окружении или в этом JDK.
3. **Попробуйте другой дистрибутив JDK 17**, например:
   - [Microsoft Build of OpenJDK 17](https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-17) (MSI x64), установите и в **Gradle JDK** укажите путь к нему (например `C:\Program Files\Microsoft\jdk-17.x.x`);
   - или [Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17) (x64 Windows).
   В `gradle.properties` при сборке из терминала можно задать новый путь: `org.gradle.java.home=C:/Program Files/Microsoft/jdk-17.x.x`.

---

## 2. AAPT2 — то же решение

Та же ошибка (AAPT2 / Universal C Runtime) устраняется установкой VC++ Redistributable — см. блок **«СРОЧНО»** в начале файла и скрипт **INSTALL_VC_RUNTIME.bat**.
