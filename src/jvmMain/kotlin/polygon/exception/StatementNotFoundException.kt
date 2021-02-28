package polygon.exception

/**
 * Statement not found exception.
 *
 * Thrown if problem statement is missing.
 */
class StatementNotFoundException(message: String) : ResourceNotFoundException(message)
