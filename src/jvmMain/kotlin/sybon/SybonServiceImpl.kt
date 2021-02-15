@file:OptIn(ExperimentalTime::class)

package sybon

import kotlinx.coroutines.delay
import sybon.api.*
import sybon.api.SybonSubmissionResult.BuildResult.Status.*
import util.encodeBase64
import kotlin.time.*

class SybonServiceImpl(
    private val sybonArchiveApi: SybonArchiveApi,
    private val sybonCheckingApi: SybonCheckingApi
) : SybonService {
    override suspend fun getCollections(offset: Int, limit: Int) = sybonArchiveApi.getCollections(offset, limit)
    override suspend fun getCollection(collectionId: Int) = sybonArchiveApi.getCollection(collectionId)
    override suspend fun getProblem(problemId: Int) = sybonArchiveApi.getProblem(problemId)
    override suspend fun getProblemStatementUrl(problemId: Int) = sybonArchiveApi.getProblemStatementUrl(problemId)

    override suspend fun getCompilers() = sybonCheckingApi.getCompilers()
    override suspend fun submitSolution(solution: SybonSubmitSolution) = sybonCheckingApi.submitSolution(solution)
    override suspend fun submitSolutions(solutions: List<SybonSubmitSolution>) = sybonCheckingApi.submitSolutions(solutions)
    override suspend fun rejudge(ids: List<Int>) = sybonCheckingApi.rejudge(ids)
    override suspend fun getResults(ids: String) = sybonCheckingApi.getResults(ids)
    override suspend fun getResult(id: Int) = sybonCheckingApi.getResults(id.toString()).single()

    override suspend fun getAllProblems() = getCollection(1).problems

    override suspend fun getProblemByBacsProblemId(bacsId: String) =
        getAllProblems().singleOrNull { it.internalProblemId == bacsId }

    override suspend fun getProblemByBacsProblemId(bacsId: String, tryFor: Duration): SybonProblem? {
        val start = TimeSource.Monotonic.markNow()
        while (start.elapsedNow() < tryFor) {
            val id = getProblemByBacsProblemId(bacsId)
            if (id != null) return id
            delay(100.milliseconds)
        }
        return null
    }

    override suspend fun submitSolution(problemId: Int, solution: String, compiler: SybonCompiler): SybonFinalSubmissionResult {
        val submissionId = submitSolution(
            SybonSubmitSolution(
                compilerId = compiler.id,
                problemId = problemId,
                solution = solution.encodeBase64(),
                continueCondition = SybonSubmitSolution.ContinueCondition.WhileOk
            )
        )

        val result = run<SybonSubmissionResult> {
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