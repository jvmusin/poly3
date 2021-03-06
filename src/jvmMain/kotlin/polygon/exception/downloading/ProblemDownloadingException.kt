package polygon.exception.downloading

import polygon.exception.PolygonException

/**
 * Problem downloading exception.
 *
 * Thrown if some error occurred while downloading the problem.
 */
open class ProblemDownloadingException(message: String, cause: Throwable? = null) : PolygonException(message, cause)
