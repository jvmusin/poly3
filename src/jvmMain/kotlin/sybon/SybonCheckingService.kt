package sybon

import sybon.api.SybonCheckingApi
import sybon.api.SybonCompiler
import sybon.api.SybonSubmissionResult
import sybon.api.SybonSubmitSolution
import util.RetryPolicy
import util.encodeBase64
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue
import kotlin.time.minutes

interface SybonCheckingService {
    suspend fun getResult(id: Int): SybonSubmissionResult

    @OptIn(ExperimentalTime::class)
    suspend fun submitSolution(
        problemId: Int,
        solution: String,
        compiler: SybonCompiler,
        checkResultRetryPolicy: RetryPolicy = RetryPolicy(tryFor = 30.minutes)
    ): SybonSubmissionResult

    @OptIn(ExperimentalTime::class)
    suspend fun submitSolutionTimed(
        problemId: Int,
        solution: String,
        compiler: SybonCompiler,
        checkResultRetryPolicy: RetryPolicy = RetryPolicy(tryFor = 30.minutes)
    ) = measureTimedValue { submitSolution(problemId, solution, compiler, checkResultRetryPolicy) }
}

class SybonCheckingServiceImpl(
    private val sybonCheckingApi: SybonCheckingApi
) : SybonCheckingService {

    override suspend fun getResult(id: Int) = sybonCheckingApi.getResults(id.toString()).single()

    override suspend fun submitSolution(
        problemId: Int,
        solution: String,
        compiler: SybonCompiler,
        checkResultRetryPolicy: RetryPolicy
    ): SybonSubmissionResult {
        val submissionId = sybonCheckingApi.submitSolution(
            SybonSubmitSolution(
                compilerId = compiler.id,
                problemId = problemId,
                solution = solution.encodeBase64(),
                continueCondition = SybonSubmitSolution.ContinueCondition.WhileOk
            )
        )

        return checkResultRetryPolicy.evalWhileNull {
            val result = getResult(submissionId)
            if (result.buildResult.status != SybonSubmissionResult.BuildResult.Status.PENDING) result
            else null
        } ?: throw SybonSolutionTestingTimeoutException("Solution was not tested in ${checkResultRetryPolicy.tryFor}")
    }
}