package sybon.converter

import api.SubmissionResult
import api.Verdict
import sybon.api.SybonSubmissionResult
import sybon.converter.SybonTestStatusToVerdictConverter.toVerdict
import util.decodeBase64
import kotlin.time.TimedValue

object SybonSubmissionResultToSubmissionResultConverter {
    private fun convert(result: SybonSubmissionResult): SubmissionResult {
        when (val status = result.buildResult.status) {
            SybonSubmissionResult.BuildResult.Status.PENDING ->
                throw SybonSubmissionResultConversionException("Converting not finished submission")
            SybonSubmissionResult.BuildResult.Status.SERVER_ERROR ->
                return SubmissionResult(Verdict.SERVER_ERROR, message = result.buildResult.output.decodeBase64())
            SybonSubmissionResult.BuildResult.Status.FAILED ->
                return SubmissionResult(Verdict.COMPILATION_ERROR, message = result.buildResult.output.decodeBase64())
            else -> check(status == SybonSubmissionResult.BuildResult.Status.OK) { "Unexpected status $status" }
        }

        val testResults = result.testGroupResults.flatMap { it.testResults }.map { it.resourceUsage }
        val maxUsedTimeMillis = testResults.maxOfOrNull { it.timeUsageMillis }
        val maxUsedMemoryBytes = testResults.maxOfOrNull { it.memoryUsageBytes }

        val failedGroupIndex = result.testGroupResults.indexOfFirst { group ->
            !group.executed || group.testResults.any { testResult ->
                testResult.status != SybonSubmissionResult.TestGroupResult.TestResult.Status.OK
            }
        }

        if (failedGroupIndex == -1) {
            checkNotNull(maxUsedTimeMillis) { "Submission has OK status, but there are no tests with measured time" }
            checkNotNull(maxUsedMemoryBytes) { "Submission has OK status, but there are no tests with measured memory" }
            return SubmissionResult.success(maxUsedTimeMillis, maxUsedMemoryBytes)
        }

        val indexInGroup = result.testGroupResults[failedGroupIndex].testResults.indexOfFirst {
            it.status != SybonSubmissionResult.TestGroupResult.TestResult.Status.OK
        }
        val testIndex = result.testGroupResults.take(failedGroupIndex).sumOf { it.testResults.size } + indexInGroup
        val testStatus = result.testGroupResults[failedGroupIndex].testResults[indexInGroup].status
        return SubmissionResult(
            verdict = testStatus.toVerdict(),
            failedTestNumber = testIndex + 1,
            maxUsedTimeMillis = maxUsedTimeMillis,
            maxUsedMemoryBytes = maxUsedMemoryBytes
        )
    }

    fun TimedValue<SybonSubmissionResult>.toSubmissionResult(): SubmissionResult {
        return convert(value).copy(executionTimeSeconds = duration.inWholeSeconds.toInt())
    }
}
