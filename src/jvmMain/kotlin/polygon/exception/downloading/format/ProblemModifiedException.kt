package polygon.exception.downloading.format

import polygon.exception.downloading.ProblemDownloadingException

/**
 * Problem modified exception.
 *
 * Thrown if the problem is modified and therefore can't be processed.
 */
class ProblemModifiedException(message: String) : ProblemDownloadingException(message)
