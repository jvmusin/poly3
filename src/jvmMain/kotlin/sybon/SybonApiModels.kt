package sybon

import kotlinx.serialization.Serializable

@Serializable
data class Collection(
    val id: Int,
    val name: String,
    val description: String,
    val problems: List<Problem>,
    val problemsCount: Int
)

@Serializable
data class Problem(
    val id: Int,
    val name: String,
    val author: String,
    val format: String,
    val statementUrl: String,
    val collectionId: Int,
    val testsCount: Int,
    val pretests: List<Test>,
    val inputFileName: String,
    val outputFileName: String,
    val internalProblemId: String,
    val resourceLimits: ResourceLimits
)

@Serializable
data class Test(
    val id: String,
    val input: String,
    val output: String
)

@Serializable
data class ResourceLimits(
    val timeLimitMillis: Int,
    val memoryLimitBytes: Int
)