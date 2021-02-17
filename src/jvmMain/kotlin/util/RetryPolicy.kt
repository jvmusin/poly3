@file:OptIn(ExperimentalTime::class)

package util

import kotlinx.coroutines.delay
import kotlin.time.*

data class RetryPolicy(
    val tryFor: Duration = 5.minutes,
    val retryAfter: Duration = 5.seconds
) {
    @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
    suspend inline fun <T> eval(func: suspend () -> T?): T? {
        val start = TimeSource.Monotonic.markNow()
        while (start.elapsedNow() < tryFor) {
            val res = func()
            if (res != null) return res
            delay(retryAfter)
        }
        return null
    }
}