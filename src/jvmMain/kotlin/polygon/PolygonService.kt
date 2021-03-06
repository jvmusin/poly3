package polygon

import ir.IRProblem
import polygon.api.PolygonApi
import polygon.api.Problem
import polygon.api.ProblemInfo
import polygon.exception.response.NoSuchProblemException
import polygon.exception.downloading.ProblemDownloadingException

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
     * @throws ProblemDownloadingException if the downloading failed.
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
    private val polygonApi: PolygonApi
) : PolygonService {

    private val polygonProblemDownloader = PolygonProblemDownloader(polygonApi)

    override suspend fun downloadProblem(problemId: Int, includeTests: Boolean): IRProblem {
        return try {
            polygonProblemDownloader.downloadProblem(problemId, includeTests)
        } catch (e: Exception) {
            throw ProblemDownloadingException("Не удалось скачать задачу: ${e.message}", e)
        }
    }

    override suspend fun getProblems() = polygonApi.getProblems().extract()
    override suspend fun getProblemInfo(problemId: Int) = polygonApi.getProblemInfo(problemId).extract()
}
