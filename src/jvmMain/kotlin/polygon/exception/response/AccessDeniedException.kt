package polygon.exception.response

/**
 * Access denied exception.
 *
 * Thrown if access to some resource is not allowed.
 */
class AccessDeniedException(message: String) : PolygonResponseException(message)
