@file:OptIn(ExperimentalTime::class)

package sybon

import sybon.api.SybonArchiveApi
import sybon.api.SybonProblem
import util.RetryPolicy
import util.getLogger
import kotlin.time.ExperimentalTime

interface SybonArchiveService {
    suspend fun getProblems(): List<SybonProblem>
    suspend fun importProblem(bacsProblemId: String, retryPolicy: RetryPolicy = RetryPolicy()): SybonProblem?
}

class SybonArchiveServiceImpl(
    private val sybonArchiveApi: SybonArchiveApi,
    private val collectionId: Int
) : SybonArchiveService {

    override suspend fun getProblems() = sybonArchiveApi.getCollection(collectionId).problems

    override suspend fun importProblem(bacsProblemId: String, retryPolicy: RetryPolicy): SybonProblem? {
        suspend fun getProblem() = getProblems().firstOrNull { it.internalProblemId == bacsProblemId }
        getLogger(javaClass).debug("Checking if the problem is already in this collection")
        getProblem()?.let {
            getLogger(javaClass).debug("The problem is already in this collection")
            return it
        }
        getLogger(javaClass).debug("The problem is not in this collection, importing")
        sybonArchiveApi.importProblem(collectionId, bacsProblemId)
        return retryPolicy.evalWhileNull(::getProblem)
    }
}