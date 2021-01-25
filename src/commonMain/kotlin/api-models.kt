@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

import kotlinx.serialization.Serializable

@Serializable
data class Problem(
    val id: Int,
    val name: String,
    val owner: String,
    val latestPackage: Int?
)

@Serializable
data class ProblemInfo(
    val inputFile: String,
    val outputFile: String,
    val interactive: Boolean,
    val timeLimitMegabytes: Int,
    val memoryLimitMillis: Int
)

fun polygon.Problem.toDto() = Problem(id, name, owner, latestPackage)
fun polygon.ProblemInfo.toDto() = ProblemInfo(inputFile, outputFile, interactive, timeLimit, memoryLimit)
