@file:OptIn(ExperimentalTime::class)

package bacs

import api.AdditionalProblemProperties
import ir.IRProblem
import kotlin.time.ExperimentalTime

interface BacsArchiveService {
    suspend fun getProblemState(problemId: String): BacsProblemState
    suspend fun uploadProblem(problem: IRProblem, properties: AdditionalProblemProperties): String
}