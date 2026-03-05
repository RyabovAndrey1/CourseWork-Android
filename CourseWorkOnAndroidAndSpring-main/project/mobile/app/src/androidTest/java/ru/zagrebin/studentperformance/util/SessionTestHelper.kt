package ru.ryabov.studentperformance.util

import androidx.test.platform.app.InstrumentationRegistry
import ru.ryabov.studentperformance.data.remote.SessionManager

/**
 * Устанавливает тестовую сессию, чтобы MainActivity не перенаправляла на экран входа.
 */
fun setTestSession() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    SessionManager(context).saveSession("test_token", 1L, "STUDENT")
}

/**
 * Очищает сессию после теста.
 */
fun clearTestSession() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    SessionManager(context).clearSession()
}
