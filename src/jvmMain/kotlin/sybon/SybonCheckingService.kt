package sybon

import sybon.api.SybonCompiler
import sybon.api.SybonSubmissionResult
import util.RetryPolicy

interface SybonCheckingService {
    suspend fun getResult(id: Int): SybonSubmissionResult
    suspend fun submitSolution(
        problemId: Int,
        solution: String,
        compiler: SybonCompiler,
        retryPolicy: RetryPolicy = RetryPolicy()
    ): SybonFinalSubmissionResult
}