package polygon.exception.downloading.packages

import polygon.exception.downloading.ProblemDownloadingException

/**
 * Package exception.
 *
 * Thrown if something is wrong with a problem package.
 */
abstract class PackageException(message: String) : ProblemDownloadingException(message)
