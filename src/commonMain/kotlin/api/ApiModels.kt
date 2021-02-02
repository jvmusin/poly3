@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED")

package api

import kotlinx.serialization.Serializable

@Serializable
data class Problem(
    val id: Int,
    val name: String,
    val owner: String,
    val accessType: AccessType,
    val latestPackage: Int?
) {
    @Serializable
    enum class AccessType {
        READ,
        WRITE,
        OWNER
    }
}

@Serializable
data class ProblemInfo(
    val inputFile: String,
    val outputFile: String,
    val interactive: Boolean,
    val timeLimitMillis: Int,
    val memoryLimitMegabytes: Int
)

@Serializable
data class AdditionalProblemProperties(
    val prefix: String? = null,
    val suffix: String? = null,
    val timeLimitMillis: Int? = null,
    val memoryLimitMegabytes: Int? = null
)
