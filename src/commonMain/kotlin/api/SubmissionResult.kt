package api

import kotlinx.serialization.Serializable

@Serializable
data class SubmissionResult(
    val verdict: Verdict,
    val failedTestNumber: Int? = null,
    val maxUsedTimeMillis: Int? = null,
    val maxUsedMemoryBytes: Int? = null,
    val executionTimeSeconds: Int? = null,
    val message: String? = null,
) {
    companion object {
        fun success(maxTimeUsedMillis: Int, maxUsedMemoryBytes: Int) =
            SubmissionResult(Verdict.OK, null, maxTimeUsedMillis, maxUsedMemoryBytes)
    }
}
