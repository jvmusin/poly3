package api

import kotlinx.serialization.Serializable

@Serializable
data class SubmissionResult(
    val compiled: Boolean,
    val failedTestIndex: Int?,
    val failedTestVerdict: Verdict?
) {
    val overallVerdict: Verdict = when {
        !compiled -> Verdict.COMPILATION_ERROR
        failedTestIndex == null -> Verdict.OK
        else -> failedTestVerdict!!
    }
}