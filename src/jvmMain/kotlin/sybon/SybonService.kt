@file:OptIn(ExperimentalTime::class)

package sybon

import kotlinx.coroutines.delay
import sybon.SubmissionResult.BuildResult.Status.*
import util.encodeBase64
import kotlin.time.*

class SybonService(
    private val sybonArchiveApi: SybonArchiveApi,
    private val sybonCheckingApi: SybonCheckingApi
) {
    suspend fun getCollections(offset: Int = 0, limit: Int = 100) = sybonArchiveApi.getCollections(offset, limit)
    suspend fun getCollection(collectionId: Int) = sybonArchiveApi.getCollection(collectionId)
    suspend fun getProblem(problemId: Int) = sybonArchiveApi.getProblem(problemId)
    suspend fun getProblemStatementUrl(problemId: Int) = sybonArchiveApi.getProblemStatementUrl(problemId)

    suspend fun getCompilers() = sybonCheckingApi.getCompilers()
    suspend fun submitSolution(solution: SubmitSolution) = sybonCheckingApi.submitSolution(solution)
    suspend fun submitSolutions(solutions: List<SubmitSolution>) = sybonCheckingApi.submitSolutions(solutions)
    suspend fun rejudge(ids: List<Int>) = sybonCheckingApi.rejudge(ids)
    suspend fun getResults(ids: String) = sybonCheckingApi.getResults(ids)
    suspend fun getResult(id: Int) = sybonCheckingApi.getResults(id.toString()).single()

    suspend fun getAllProblems() = getCollection(1).problems

    suspend fun getProblemByBacsProblemId(bacsId: String) =
        getAllProblems().singleOrNull { it.internalProblemId == bacsId }

    suspend fun getProblemByBacsProblemId(bacsId: String, tryFor: Duration): Problem? {
        val start = TimeSource.Monotonic.markNow()
        while (start.elapsedNow() < tryFor) {
            val id = getProblemByBacsProblemId(bacsId)
            if (id != null) return id
            delay(100.milliseconds)
        }
        return null
    }

    suspend fun submitSolution(problemId: Int, solution: String, compiler: Compiler): SybonFinalSubmissionResult {
        val submissionId = submitSolution(
            SubmitSolution(
                compilerId = compiler.id,
                problemId = problemId,
                solution = solution.encodeBase64(),
                continueCondition = SubmitSolution.ContinueCondition.WhileOk
            )
        )

        val result = run<SubmissionResult> {
            while (true) {
                delay(1.seconds)
                val result = getResult(submissionId)
                if (result.buildResult.status != PENDING) return@run result
            }
            @Suppress("ThrowableNotThrown", "UNREACHABLE_CODE")
            throw Error("Never here")
        }

        val failedGroupIndex = result.testGroupResults.indexOfFirst { group ->
            !group.executed || group.testResults.any { testResult ->
                testResult.status != SubmissionResult.TestGroupResult.TestResult.Status.OK
            }
        }
        if (failedGroupIndex == -1) return SybonFinalSubmissionResult(true, null, null)
        val indexInGroup = result.testGroupResults[failedGroupIndex].testResults.indexOfFirst {
            it.status != SubmissionResult.TestGroupResult.TestResult.Status.OK
        }
        val testIndex = result.testGroupResults.take(failedGroupIndex).sumOf { it.testResults.size } + indexInGroup
        val testStatus = result.testGroupResults[failedGroupIndex].testResults[indexInGroup].status
        return SybonFinalSubmissionResult(true, testIndex, testStatus)
    }
}