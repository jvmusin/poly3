package polygon.exception

/**
 * Problem modified exception.
 *
 * Thrown if the problem is modified and therefore can't be processed.
 */
class ProblemModifiedException(message: String) : PolygonException(message)
