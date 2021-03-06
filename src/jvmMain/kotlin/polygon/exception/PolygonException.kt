package polygon.exception

import ConverterException

/**
 * Polygon exception.
 *
 * Thrown if something bad happened to Polygon.
 */
abstract class PolygonException(message: String, cause: Throwable? = null) : ConverterException(message, cause)
