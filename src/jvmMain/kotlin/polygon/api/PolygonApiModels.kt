@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED", "unused")

package polygon.api

import kotlinx.serialization.Serializable
import polygon.exception.ResultExtractingException

@Serializable
data class PolygonResponse<T>(
    val status: String,
    val result: T? = null,
    val comment: String? = null
) {
    /**
     * Extracts [result] from the response.
     *
     * If [result] is null, throws [ResultExtractingException] with [comment] as an exception message.
     *
     * @return Unboxed *non-null* [result].
     * @throws ResultExtractingException if [result] is *null*.
     */
    fun extract() = result ?: throw ResultExtractingException(comment)
}

@Serializable
data class Problem(
    val id: Int,
    val owner: String,
    val name: String,
    val deleted: Boolean,
    val favourite: Boolean,
    val accessType: AccessType,
    val revision: Int,
    val latestPackage: Int? = null,
    val modified: Boolean
) {
    @Serializable
    enum class AccessType {
        READ,
        WRITE,
        OWNER
    }
}

/**
 * Problem info.
 *
 * @property inputFile Input file name or **stdin** if no input file is used.
 * @property outputFile Output file name or **stdout** if no output file is used.
 * @property interactive Whether the problem is interactive or not.
 * @property timeLimit Time limit in milliseconds.
 * @property memoryLimit Memory limit in megabytes.
 */
@Serializable
data class ProblemInfo(
    val inputFile: String,
    val outputFile: String,
    val interactive: Boolean,
    val timeLimit: Int,
    val memoryLimit: Int
)

@Serializable
data class Statement(
    val encoding: String,
    val name: String,
    val legend: String,
    val input: String,
    val output: String,
    val scoring: String? = null,
    val notes: String,
    val tutorial: String
)

@Serializable
data class File(
    val name: String,
    val modificationTimeSeconds: Long,
    val length: Long,
    val sourceType: String? = null,
    val resourceAdvancedProperties: ResourceAdvancedProperties? = null
) {
    enum class Type {
        RESOURCE,
        SOURCE,
        AUX;

        override fun toString() = super.toString().toLowerCase()
    }
}

@Serializable
data class ResourceAdvancedProperties(
    val forTypes: String,
    val main: String,
    val stages: List<StageType>,
    val assets: List<AssetType>
) {
    @Serializable
    enum class StageType {
        COMPILE,
        RUN
    }

    @Serializable
    enum class AssetType {
        VALIDATOR,
        INTERACTOR,
        CHECKER,
        SOLUTION
    }
}

@Serializable
data class Solution(
    val name: String,
    val modificationTimeSeconds: Int,
    val length: Int,
    val sourceType: String,
    val tag: String
) {
    val isMain get() = tag == "MA"
}

@Serializable
data class PolygonTest(
    val index: Int,
    val manual: Boolean,
    val input: String? = null,
    val description: String? = null,
    val useInStatements: Boolean,
    val scriptLine: String? = null,
    val group: String? = null,
    val points: Double? = null,
    val inputForStatement: String? = null,
    val outputForStatement: String? = null,
    val verifyInputOutputForStatements: Boolean? = null
)

@Serializable
data class TestGroup(
    val name: String,
    val pointsPolicy: PointsPolicyType,
    val feedbackPolicy: String, // add enums
    val dependencies: List<String>
) {
    @Serializable
    enum class PointsPolicyType {
        COMPLETE_GROUP,
        EACH_TEST
    }
}

@Serializable
data class Package(
    val id: Int,
    val creationTimeSeconds: Long,
    val state: State,
    val comment: String,
    val revision: Int
) {
    @Serializable
    enum class State {
        PENDING,
        RUNNING,
        READY,
        FAILED
    }
}