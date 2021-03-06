package polygon.exception.response

/**
 * No such test group exception.
 *
 * Thrown if requested test group does not exist.
 */
class NoSuchTestGroupException(message: String) : PolygonResponseException(message)
