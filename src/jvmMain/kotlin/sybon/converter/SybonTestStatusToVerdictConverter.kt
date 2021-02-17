package sybon.converter

import api.Verdict
import sybon.api.SybonSubmissionResult
import sybon.api.SybonSubmissionResult.TestGroupResult.TestResult.Status.*

object SybonTestStatusToVerdictConverter {
    private fun convert(status: SybonSubmissionResult.TestGroupResult.TestResult.Status): Verdict {
        return when (status) {
            OK -> Verdict.OK
            WRONG_ANSWER -> Verdict.WRONG_ANSWER
            TIME_LIMIT_EXCEEDED -> Verdict.TIME_LIMIT_EXCEEDED
            MEMORY_LIMIT_EXCEEDED -> Verdict.MEMORY_LIMIT_EXCEEDED
            PRESENTATION_ERROR -> Verdict.PRESENTATION_ERROR
            ABNORMAL_EXIT -> Verdict.ABNORMAL_EXIT
            else -> Verdict.INCORRECT
        }
    }

    fun SybonSubmissionResult.TestGroupResult.TestResult.Status.toVerdict() = convert(this)
}