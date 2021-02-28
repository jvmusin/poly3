package polygon.exception

/**
 * No such problem exception.
 *
 * Thrown if the problem is not found or if access to the problem is denied.
 */
class NoSuchProblemException(message: String? = null, cause: Throwable? = null) : PolygonException(message, cause)
