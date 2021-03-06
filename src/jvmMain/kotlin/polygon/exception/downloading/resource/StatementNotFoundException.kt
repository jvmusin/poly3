package polygon.exception.downloading.resource

/**
 * Statement not found exception.
 *
 * Thrown if problem statement is missing.
 */
open class StatementNotFoundException(message: String) : ResourceNotFoundException(message)
