package api

import api.StatementFormat.PDF
import kotlinx.serialization.Serializable

/**
 * Additional problem properties.
 *
 * Used for modifying problem name or time/memory limits when uploading problems to external systems.
 *
 * @param prefix prefix to add to problem name or `null` for no extra prefix.
 * @param suffix suffix to add to problem name or `null` for no extra suffix.
 * @param timeLimitMillis time limit in millis to set or `null` for problem's default time limit.
 * @param memoryLimitMegabytes memory limit in megabytes to set or `null` for problem's default memory limit.
 * @param statementFormat format of the statement, actually `PDF` or `HTML`.
 */
@Serializable
data class AdditionalProblemProperties(
    val prefix: String? = null,
    val suffix: String? = null,
    val timeLimitMillis: Int? = null,
    val memoryLimitMegabytes: Int? = null,
    val statementFormat: StatementFormat = PDF
) {
    companion object {
        /** Do not add any prefix/suffix and use problem's default time and memory limits. */
        val defaultProperties = AdditionalProblemProperties()
    }

    /** Build problem name prefixing it with [prefix] and suffixing with [suffix] if they are not `null`. */
    fun buildFullName(problemName: String) = "${prefix.orEmpty()}$problemName${suffix.orEmpty()}"
}
