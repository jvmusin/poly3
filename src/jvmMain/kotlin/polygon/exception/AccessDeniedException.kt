package polygon.exception

/**
 * Access denied exception.
 *
 * Thrown if access to some resource is not allowed.
 */
class AccessDeniedException(message: String? = null, cause: Throwable? = null) : PolygonException(message, cause)
