@file:OptIn(ExperimentalTime::class)

package bacs

import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

interface BacsArchiveService {
    suspend fun uploadProblem(zip: Path)
    suspend fun getProblemState(problemId: String): BacsProblemState
    suspend fun waitTillProblemIsImported(problemId: String, waitFor: Duration): BacsProblemStatus
}