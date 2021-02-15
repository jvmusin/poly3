package sybon

import kotlinx.serialization.Serializable
import sybon.api.SybonSubmissionResult

@Serializable
data class SybonFinalSubmissionResult(
    val compiled: Boolean,
    val failedTest: Int?,
    val failedTestStatus: SybonSubmissionResult.TestGroupResult.TestResult.Status?
)