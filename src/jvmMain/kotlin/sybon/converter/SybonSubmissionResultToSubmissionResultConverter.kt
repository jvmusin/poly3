package sybon.converter

import api.SubmissionResult
import sybon.SybonFinalSubmissionResult
import sybon.converter.SybonTestStatusToVerdictConverter.toVerdict

object SybonSubmissionResultToSubmissionResultConverter {
    private fun convert(result: SybonFinalSubmissionResult): SubmissionResult {
        return SubmissionResult(
            result.compiled,
            result.failedTest,
            result.failedTestStatus?.toVerdict()
        )
    }

    fun SybonFinalSubmissionResult.toSubmissionResult() = convert(this)
}