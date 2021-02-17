@file:OptIn(ExperimentalTime::class)

package sybon

import sybon.api.SybonProblem
import util.RetryPolicy
import kotlin.time.ExperimentalTime

interface SybonArchiveService {
    suspend fun getProblems(): List<SybonProblem>
    suspend fun importProblem(bacsProblemId: String, retryPolicy: RetryPolicy = RetryPolicy()): SybonProblem?
}