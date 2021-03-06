@file:OptIn(ExperimentalTime::class)

package util

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.minutes
import kotlin.time.seconds

/**
 * Retry policy.
 *
 * Allows to retry some action for the given period of time with some delay until some condition is met.
 *
 * @property tryFor Period of time to try to evaluate the function.
 * @property retryAfter Period of time to wait until the next try after fail.
 */
data class RetryPolicy(
    val tryFor: Duration = 5.minutes,
    val retryAfter: Duration = 5.seconds
) {

    /**
     * Evaluates the given [function] at least once.
     * Returns result as soon as [condition] returns *true* for the first time
     * or when [tryFor] period is gone.
     *
     * Does retries every [retryAfter] until [tryFor] time is gone.
     *
     * The returned value is either the result of [function] when [condition] returned *true* for the first time or
     * the last calculated by [function] value if [tryFor] time is gone.
     *
     * @param T type of the result.
     * @param condition function to check if the result is correct and can be returned.
     * @param function function to generate result.
     * @return First successful or last calculated result.
     */
    @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
    suspend inline fun <T> evalWhileFails(condition: (T) -> Boolean, function: suspend () -> T): T {
        val start = TimeSource.Monotonic.markNow()
        var first = true
        var res: T? = null
        while (first || start.elapsedNow() < tryFor) {
            first = false
            res = function()
            if (condition(res)) return res
            delay(retryAfter)
        }
        return res!!
    }

    /**
     * Evaluates the given [function] at least once.
     * Returns result as soon as the result is not *null* for the first time
     * or when [tryFor] period is gone.
     *
     * Does retries every [retryAfter] until [tryFor] time is gone.
     *
     * The returned value is either the result of [function] when it's not *null* for the first time or
     * the last calculated by [function] value if [tryFor] time is gone.
     *
     * @param T type of the result.
     * @param function function to generate result.
     * @return First not null or last calculated result.
     */
    @Suppress("REDUNDANT_INLINE_SUSPEND_FUNCTION_TYPE")
    suspend inline fun <T> evalWhileNull(function: suspend () -> T?) = evalWhileFails({ it != null }, function)
}
