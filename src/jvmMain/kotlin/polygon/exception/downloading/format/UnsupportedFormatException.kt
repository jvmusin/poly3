package polygon.exception.downloading.format

import polygon.exception.downloading.ProblemDownloadingException

/**
 * Format not supported exception.
 *
 * Thrown if problem format is not supported for some reason.
 */
class UnsupportedFormatException(message: String) : ProblemDownloadingException(message)
