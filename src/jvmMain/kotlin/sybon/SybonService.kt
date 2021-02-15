@file:OptIn(ExperimentalTime::class)

package sybon

import sybon.api.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

interface SybonService {
    suspend fun getCollections(offset: Int = 0, limit: Int = 100): List<SybonCollection>
    suspend fun getCollection(collectionId: Int): SybonCollection
    suspend fun getProblem(problemId: Int): SybonProblem
    suspend fun getProblemStatementUrl(problemId: Int): String

    suspend fun getCompilers(): List<SybonCompiler>
    suspend fun submitSolution(solution: SybonSubmitSolution): Int
    suspend fun submitSolutions(solutions: List<SybonSubmitSolution>): List<Int>
    suspend fun rejudge(ids: List<Int>)
    suspend fun getResults(ids: String): List<SybonSubmissionResult>
    suspend fun getResult(id: Int): SybonSubmissionResult

    suspend fun getAllProblems(): List<SybonProblem>
    suspend fun getProblemByBacsProblemId(bacsId: String): SybonProblem?
    suspend fun getProblemByBacsProblemId(bacsId: String, tryFor: Duration): SybonProblem?
    suspend fun submitSolution(problemId: Int, solution: String, compiler: SybonCompiler): SybonFinalSubmissionResult
}