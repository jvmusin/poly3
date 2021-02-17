@file:OptIn(ExperimentalTime::class)

package util

import kotlinx.coroutines.delay
import kotlin.time.*

data class RetryPolicy(
    val tryFor: Duration = 5.minutes,
    val retryAfter: Duration = 5.seconds
) {

    @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
    suspend inline fun <T> evalWhileFails(check: (T?) -> Boolean, func: suspend () -> T?): T? {
        val start = TimeSource.Monotonic.markNow()
        var first = true
        var res: T? = null
        while (first || start.elapsedNow() < tryFor) {
            first = false
            res = func()
            if (check(res)) return res
            delay(retryAfter)
        }
        return res
    }

    @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
    suspend inline fun <T> evalWhileNull(func: suspend () -> T?) = evalWhileFails({ it != null }, func)
}