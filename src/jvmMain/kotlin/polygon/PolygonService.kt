package polygon

import ir.IRProblem
import polygon.api.PolygonApi
import polygon.exception.downloading.ProblemDownloadingException
import polygon.exception.response.AccessDeniedException
import polygon.exception.response.NoSuchProblemException

/**
 * Polygon service.
 *
 * Used to communicate to the Polygon API.
 */
class PolygonService(
    private val polygonApi: PolygonApi,
    private val problemDownloader: PolygonProblemDownloader
) {

    /**
     * Downloads problem with the given [problemId] and skips tests if [includeTests] is `false` (default).
     *
     * @throws NoSuchProblemException if the problem does not exist.
     * @throws AccessDeniedException if not enough rights to download the problem.
     * @throws ProblemDownloadingException if something went wrong while downloading the problem.
     */
    suspend fun downloadProblem(problemId: Int, includeTests: Boolean = false): IRProblem {
        return try {
            problemDownloader.downloadProblem(problemId, includeTests)
        } catch (e: Exception) {
            throw ProblemDownloadingException("Не удалось скачать задачу: ${e.message}", e)
        }
    }

    /** Returns all known problems. */
    suspend fun getProblems() = polygonApi.getProblems().extract()

    /**
     * Returns problem information for the problem with the given [problemId].
     *
     * @throws NoSuchProblemException if the problem is not found or if access to the problem is denied.
     */
    suspend fun getProblemInfo(problemId: Int) = polygonApi.getProblemInfo(problemId).extract()
}
