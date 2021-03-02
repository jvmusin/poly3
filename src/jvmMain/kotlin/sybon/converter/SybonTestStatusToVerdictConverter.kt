package sybon.converter

import api.Verdict
import sybon.api.SybonSubmissionResult.TestGroupResult.TestResult.Status

object SybonTestStatusToVerdictConverter {
    private fun convert(status: Status): Verdict {
        return when (status) {
            Status.OK -> Verdict.OK
            Status.WRONG_ANSWER -> Verdict.WRONG_ANSWER
            Status.TIME_LIMIT_EXCEEDED -> Verdict.TIME_LIMIT_EXCEEDED
            Status.MEMORY_LIMIT_EXCEEDED -> Verdict.MEMORY_LIMIT_EXCEEDED
            Status.PRESENTATION_ERROR -> Verdict.PRESENTATION_ERROR
            Status.ABNORMAL_EXIT -> Verdict.ABNORMAL_EXIT
            else -> Verdict.INCORRECT
        }
    }

    fun Status.toVerdict() = convert(this)
}
