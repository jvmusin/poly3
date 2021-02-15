package sybon

import kotlinx.serialization.Serializable

@Serializable
data class SybonFinalSubmissionResult(
    val compiled: Boolean,
    val failedTest: Int?,
    val failedTestStatus: SubmissionResult.TestGroupResult.TestResult.Status?
)