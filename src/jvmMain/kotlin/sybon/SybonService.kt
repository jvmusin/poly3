package sybon

import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.milliseconds

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
    suspend fun getResult(id: Int) = sybonCheckingApi.getResults(id.toString())

    suspend fun getAllProblems() = getCollection(1).problems

    suspend fun getProblemByBacsProblemId(bacsId: String) =
        getAllProblems().singleOrNull { it.internalProblemId == bacsId }

    @OptIn(ExperimentalTime::class)
    suspend fun getProblemByBacsProblemId(bacsId: String, tryFor: Duration): Problem? {
        val start = TimeSource.Monotonic.markNow()
        while (start.elapsedNow() < tryFor) {
            val id = getProblemByBacsProblemId(bacsId)
            if (id != null) return id
            delay(100.milliseconds)
        }
        return null
    }
}