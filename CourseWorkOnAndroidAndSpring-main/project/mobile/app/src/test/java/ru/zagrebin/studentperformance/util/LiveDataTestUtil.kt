package ru.ryabov.studentperformance.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Получить значение LiveData, дожидаясь не более [timeout] единиц [unit].
 * Используется в unit-тестах для проверки состояния после асинхронных операций.
 */
fun <T> LiveData<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS
): T? {
    val data = AtomicReference<T?>()
    val latch = CountDownLatch(1)
    val observer = Observer<T> { value ->
        data.set(value)
        latch.countDown()
    }
    try {
        observeForever(observer)
        if (!latch.await(time, timeUnit)) {
            return data.get()
        }
        return data.get()
    } finally {
        removeObserver(observer)
    }
}
