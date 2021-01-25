@file:Suppress("RUNTIME_ANNOTATION_NOT_SUPPORTED", "unused")

package polygon

import kotlinx.serialization.Serializable

@Serializable
data class CodeforcesResponse<T>(
    val status: String,
    val result: T? = null
)

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
)

@Serializable
data class Test(
    val index: Int,
    val manual: Boolean,
    val input: String? = null,
    val description: String? = null,
    val useInStatements: Boolean,
    val scriptLine: String? = null,
    val group: String? = null,
    val points: Double? = null,
    val inputForStatements: String? = null,
    val outputForStatements: String? = null,
    val verifyInputOutputForStatements: Boolean? = null
)

@Serializable
data class TestGroup(
    val name: String,
    val pointsPolicy: PointsPolicyType,
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