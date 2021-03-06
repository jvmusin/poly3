package polygon

import ir.IRProblem
import polygon.api.PolygonApi
import polygon.api.Problem
import polygon.api.ProblemInfo
import polygon.exception.downloading.ProblemDownloadingException
import polygon.exception.response.AccessDeniedException
import polygon.exception.response.NoSuchProblemException

/**
 * Polygon service.
 *
 * Used to communicate to the Polygon API.
 */
interface PolygonService {
    /**
     * Downloads the problem with the given [problemId].
     *
     * Tests might be skipped by setting [includeTests] to *false* (they are skipped by default).
     *
     * @param problemId id of the problem to download.
     * @param includeTests if true then the problem tests will also be downloaded.
     * @return The problem with or without tests, depending on [includeTests] parameter.
     * @throws NoSuchProblemException if the problem does not exist.
     * @throws AccessDeniedException if not enough rights to download the problem.
     * @throws ProblemDownloadingException if something gone wrong while downloading the problem.
     */
    suspend fun downloadProblem(problemId: Int, includeTests: Boolean = false): IRProblem

    /**
     * Returns all known problems.
     *
     * @return The list of all known problems.
     */
    suspend fun getProblems(): List<Problem>

    /**
     * Returns problem information for the problem with the given [problemId].
     *
     * @param problemId problem id to return information for.
     * @return Problem information.
     * @throws NoSuchProblemException if the problem is not found or if access to the problem is denied.
     */
    suspend fun getProblemInfo(problemId: Int): ProblemInfo
}

class PolygonServiceImpl(
    private val polygonApi: PolygonApi,
    private val problemDownloader: PolygonProblemDownloader
) : PolygonService {

    override suspend fun downloadProblem(problemId: Int, includeTests: Boolean): IRProblem {
        return try {
            problemDownloader.downloadProblem(problemId, includeTests)
        } catch (e: Exception) {
            throw ProblemDownloadingException("Не удалось скачать задачу: ${e.message}", e)
        }
    }

    override suspend fun getProblems() = polygonApi.getProblems().extract()
    override suspend fun getProblemInfo(problemId: Int) = polygonApi.getProblemInfo(problemId).extract()
}
