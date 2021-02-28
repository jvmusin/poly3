package polygon.exception

import ConverterException

/**
 * Polygon exception.
 *
 * Thrown if something bad happened to Polygon.
 */
abstract class PolygonException(message: String? = null, cause: Throwable? = null) : ConverterException(message, cause)
