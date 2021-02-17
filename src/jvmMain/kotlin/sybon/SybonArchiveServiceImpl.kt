@file:OptIn(ExperimentalTime::class)

package sybon

import sybon.api.SybonArchiveApi
import sybon.api.SybonProblem
import util.RetryPolicy
import util.getLogger
import kotlin.time.ExperimentalTime

class SybonArchiveServiceImpl(
    private val sybonArchiveApi: SybonArchiveApi,
    private val collectionId: Int
) : SybonArchiveService {

    override suspend fun getProblems() = sybonArchiveApi.getCollection(collectionId).problems

    override suspend fun importProblem(bacsProblemId: String, retryPolicy: RetryPolicy): SybonProblem? {
        getLogger(javaClass).debug("Checking if the problem is already in this collection")
        getProblems().singleOrNull { it.internalProblemId == bacsProblemId }?.let {
            getLogger(javaClass).debug("The problem is already in this collection")
            return it
        }
        getLogger(javaClass).debug("The problem is not in this collection, importing")
        sybonArchiveApi.importProblem(collectionId, bacsProblemId)
        return retryPolicy.eval {
            getProblems().singleOrNull { it.internalProblemId == bacsProblemId }
        }
    }
}