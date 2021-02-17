package sybon

import sybon.api.SybonCheckingApi
import sybon.api.SybonCompiler
import sybon.api.SybonSubmissionResult
import sybon.api.SybonSubmitSolution
import util.RetryPolicy
import util.encodeBase64

class SybonCheckingServiceImpl(
    private val sybonCheckingApi: SybonCheckingApi
) : SybonCheckingService {

    override suspend fun getResult(id: Int) = sybonCheckingApi.getResults(id.toString()).single()

    override suspend fun submitSolution(
        problemId: Int,
        solution: String,
        compiler: SybonCompiler,
        retryPolicy: RetryPolicy
    ): SybonFinalSubmissionResult {
        val submissionId = sybonCheckingApi.submitSolution(
            SybonSubmitSolution(
                compilerId = compiler.id,
                problemId = problemId,
                solution = solution.encodeBase64(),
                continueCondition = SybonSubmitSolution.ContinueCondition.WhileOk
            )
        )

        val result = retryPolicy.eval {
            val result = getResult(submissionId)
            if (result.buildResult.status != SybonSubmissionResult.BuildResult.Status.PENDING) result
            else null
        } ?: throw SybonSubmitSolutionException("Решение не было отправлено")

        val failedGroupIndex = result.testGroupResults.indexOfFirst { group ->
            !group.executed || group.testResults.any { testResult ->
                testResult.status != SybonSubmissionResult.TestGroupResult.TestResult.Status.OK
            }
        }
        if (failedGroupIndex == -1) return SybonFinalSubmissionResult(true, null, null)
        val indexInGroup = result.testGroupResults[failedGroupIndex].testResults.indexOfFirst {
            it.status != SybonSubmissionResult.TestGroupResult.TestResult.Status.OK
        }
        val testIndex = result.testGroupResults.take(failedGroupIndex).sumOf { it.testResults.size } + indexInGroup
        val testStatus = result.testGroupResults[failedGroupIndex].testResults[indexInGroup].status
        return SybonFinalSubmissionResult(true, testIndex, testStatus)
    }
}