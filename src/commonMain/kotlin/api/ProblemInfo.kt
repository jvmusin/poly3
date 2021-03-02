package api

import kotlinx.serialization.Serializable

@Serializable
data class ProblemInfo(
    val inputFile: String,
    val outputFile: String,
    val interactive: Boolean,
    val timeLimitMillis: Int,
    val memoryLimitMegabytes: Int
)
