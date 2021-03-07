package polygon.api

import kotlinx.serialization.Serializable
import polygon.exception.response.NoSuchProblemException
import polygon.exception.response.NoSuchTestGroupException
import polygon.exception.response.PolygonResponseException
import polygon.exception.response.TestGroupsDisabledException

/**
 * Polygon response.
 *
 * Represents Polygon response.
 * If [result] is *not null*, then it can be taken.
 * Otherwise, the request failed and the reason is explained in [comment].
 *
 * @param T type of the [result].
 * @property status Might be **OK** or **FAILED** depending on whether request succeeded or not.
 * @property result The result of the request or *null* if request failed.
 * @property comment The reason of failure or *null* if request succeeded.
 */
@Serializable
data class PolygonResponse<T>(
    val status: String,
    val result: T? = null,
    val comment: String? = null
) {
    /**
     * Extracts [result] from the response.
     *
     * @return Unboxed *non-null* [result].
     * @throws NoSuchProblemException if some problem was requested but was not found.
     * @throws TestGroupsDisabledException if test groups are disabled for the problem/testset.
     * @throws NoSuchTestGroupException if test group does not exist.
     * @throws PolygonResponseException if [result] is *null*.
     */
    fun extract() = when {
        result != null -> result
        else -> when (comment) {
            null -> throw PolygonResponseException("Comment is null! Please check this out!")
            "problemId: Problem not found" -> throw NoSuchProblemException("Problem not found")
            "testset: Test groups are disabled for the specified testset" ->
                throw TestGroupsDisabledException("Test groups are disabled for the specified testset")
            "group: No test group with specified name in the specified testset" ->
                throw NoSuchTestGroupException("Test group not found")
            else -> throw PolygonResponseException(comment)
        }
    }
}
