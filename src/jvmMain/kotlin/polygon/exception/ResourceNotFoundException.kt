package polygon.exception

/**
 * Resource not found exception.
 *
 * Thrown if some required problem resource is missing.
 */
abstract class ResourceNotFoundException(message: String) : PolygonException(message)
