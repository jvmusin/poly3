package polygon.exception.response

import polygon.api.PolygonResponse
import polygon.exception.PolygonException

/**
 * PolygonResponse result extracting exception.
 *
 * Thrown if [PolygonResponse.result] is *null*.
 *
 * @param comment message taken from [PolygonResponse.comment].
 */
open class PolygonResponseException(comment: String) : PolygonException(comment)
