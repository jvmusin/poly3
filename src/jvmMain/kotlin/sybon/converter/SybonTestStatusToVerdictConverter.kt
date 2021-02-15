package sybon.converter

import api.Verdict
import sybon.SubmissionResult
import sybon.SubmissionResult.TestGroupResult.TestResult.Status.*

object SybonTestStatusToVerdictConverter {
    private fun convert(status: SubmissionResult.TestGroupResult.TestResult.Status): Verdict {
        return when (status) {
            OK -> Verdict.OK
            WRONG_ANSWER -> Verdict.WRONG_ANSWER
            TIME_LIMIT_EXCEEDED -> Verdict.TIME_LIMIT_EXCEEDED
            MEMORY_LIMIT_EXCEEDED -> Verdict.MEMORY_LIMIT_EXCEEDED
            PRESENTATION_ERROR -> Verdict.PRESENTATION_ERROR
            else -> Verdict.INCORRECT
        }
    }

    fun SubmissionResult.TestGroupResult.TestResult.Status.toVerdict() = convert(this)
}