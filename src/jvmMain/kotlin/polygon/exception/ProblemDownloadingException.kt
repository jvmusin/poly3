package polygon.exception

/**
 * Problem downloading exception.
 *
 * Thrown if some error occurred while downloading the problem.
 */
class ProblemDownloadingException(message: String? = null, cause: Throwable? = null) : PolygonException(message, cause)
