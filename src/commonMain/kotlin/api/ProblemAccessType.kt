package api

import kotlinx.serialization.Serializable

@Serializable
enum class ProblemAccessType(val isSufficient: Boolean) {
    READ(false),
    WRITE(true),
    OWNER(true);

    val notSufficient = !isSufficient
}
