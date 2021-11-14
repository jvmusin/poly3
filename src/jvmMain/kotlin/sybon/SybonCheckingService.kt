package sybon

import sybon.api.SybonCheckingApi
import sybon.api.SybonCompiler
import sybon.api.SybonSubmissionResult
import sybon.api.SybonSubmitSolution
import util.RetryPolicy
import util.encodeBase64
import kotlin.time.Duration.Companion.minutes
import kotlin.time.measureTimedValue

/**
 * Sybon checking service.
 *
 * Allows submitting and retrieving submission results from Sybon.
 *
 * @property sybonCheckingApi api to use.
 */
@Suppress("MemberVisibilityCanBePrivate")
class SybonCheckingService(private val sybonCheckingApi: SybonCheckingApi) {

    /**
     * Returns [SybonSubmissionResult] of submission with the given [id].
     */
    suspend fun getResult(id: Int) = sybonCheckingApi.getResults(id.toString()).single()

    /**
     * Submits solution with the given [solutionText] to the problem with the given [problemId], compiling under [compiler].
     *
     * Retrieves solution result accordingly to [checkResultRetryPolicy] and returns [SybonSubmissionResult] when it's ready.
     */
    suspend fun submitSolution(
        problemId: Int,
        solutionText: String,
        compiler: SybonCompiler,
        checkResultRetryPolicy: RetryPolicy
    ): SybonSubmissionResult {
        val submissionId = sybonCheckingApi.submitSolution(
            SybonSubmitSolution(
                compilerId = compiler.id,
                problemId = problemId,
                solution = solutionText.encodeBase64(),
                continueCondition = SybonSubmitSolution.ContinueCondition.WhileOk
            )
        )

        return checkResultRetryPolicy.evalWhileNull {
            val result = getResult(submissionId)
            if (result.buildResult.status != SybonSubmissionResult.BuildResult.Status.PENDING) result
            else null
        } ?: throw SybonSolutionTestingTimeoutException("Solution was not tested in ${checkResultRetryPolicy.tryFor}")
    }

    /**
     * Submits solution with the given [solutionText] to the problem with the given [problemId] using the given [compiler].
     *
     * Retrieves solution result accordingly to [checkResultRetryPolicy] and returns [SybonSubmissionResult] when it's ready.
     */
    suspend fun submitSolutionTimed(
        problemId: Int,
        solutionText: String,
        compiler: SybonCompiler,
        checkResultRetryPolicy: RetryPolicy = RetryPolicy(tryFor = 30.minutes)
    ) = measureTimedValue { submitSolution(problemId, solutionText, compiler, checkResultRetryPolicy) }
}
