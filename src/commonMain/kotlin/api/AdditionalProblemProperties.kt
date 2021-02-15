package api

import kotlinx.serialization.Serializable

@Serializable
data class AdditionalProblemProperties(
    val prefix: String? = null,
    val suffix: String? = null,
    val timeLimitMillis: Int? = null,
    val memoryLimitMegabytes: Int? = null
) {
    fun buildFullName(problemName: String) = "${prefix.orEmpty()}$problemName${suffix.orEmpty()}"
}