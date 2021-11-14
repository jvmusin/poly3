package polygon

import api.StatementFormat
import ir.IRProblem
import polygon.api.PolygonApi
import polygon.exception.downloading.ProblemDownloadingException
import polygon.exception.response.AccessDeniedException
import polygon.exception.response.NoSuchProblemException

/**
 * Polygon service.
 *
 * Used to communicate to Polygon API.
 */
class PolygonService(
    private val polygonApi: PolygonApi,
    private val problemDownloader: PolygonProblemDownloader
) {

    /**
     * Downloads problem with the given [problemId] and skips tests if [includeTests] is `false` (default).
     * Additionally, you can choose a [statementFormat] between `PDF` and `HTML`.
     *
     * @throws NoSuchProblemException if the problem does not exist.
     * @throws AccessDeniedException if not enough rights to download the problem.
     * @throws ProblemDownloadingException if something went wrong while downloading the problem.
     */
    suspend fun downloadProblem(
        problemId: Int,
        includeTests: Boolean = false,
        statementFormat: StatementFormat = StatementFormat.PDF
    ): IRProblem {
        try {
            return problemDownloader.downloadProblem(problemId, includeTests, statementFormat)
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
