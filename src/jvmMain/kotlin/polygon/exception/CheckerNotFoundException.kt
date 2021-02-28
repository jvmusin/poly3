package polygon.exception

/**
 * Checker not found exception.
 *
 * Thrown if checker for the problem is not found.
 */
class CheckerNotFoundException(message: String) : ResourceNotFoundException(message)
