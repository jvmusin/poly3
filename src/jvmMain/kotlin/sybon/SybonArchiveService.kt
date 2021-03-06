@file:OptIn(ExperimentalTime::class)

package sybon

import org.slf4j.LoggerFactory.getLogger
import retrofit2.HttpException
import sybon.api.SybonArchiveApi
import sybon.api.SybonProblem
import util.RetryPolicy
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
        suspend fun getProblem() = getProblems().filter { it.internalProblemId == bacsProblemId }.minByOrNull { it.id }
        getLogger(javaClass).debug("Checking if the problem is already in this collection")
        getProblem()?.let {
            getLogger(javaClass).debug("The problem is already in this collection")
            return it
        }
        getLogger(javaClass).debug("The problem is not in this collection, importing")
        retryPolicy.evalWhileNull {
            try {
                sybonArchiveApi.importProblem(collectionId, bacsProblemId)
                getLogger(javaClass).debug("Problem import into sybon succeed")
            } catch (e: HttpException) {
                if (e.code() == 500) {
                    getLogger(javaClass)
                        .debug("Sybon returned 500 error on problem import, will try again")
                    null
                } else throw SybonProblemImportException("Problem import into sybon failed: ${e.message()}", e)
            }
        }

        sybonArchiveApi.importProblem(collectionId, bacsProblemId)
        return retryPolicy.evalWhileNull(::getProblem)
    }
}
