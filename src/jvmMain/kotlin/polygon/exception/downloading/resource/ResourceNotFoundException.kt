package polygon.exception.downloading.resource

import polygon.exception.downloading.ProblemDownloadingException

/**
 * Resource not found exception.
 *
 * Thrown if some required problem resource is missing.
 */
abstract class ResourceNotFoundException(message: String) : ProblemDownloadingException(message)
