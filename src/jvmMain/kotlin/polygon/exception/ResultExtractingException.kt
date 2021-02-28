package polygon.exception

import polygon.api.PolygonResponse

/**
 * PolygonResponse result extracting exception.
 *
 * Thrown if [PolygonResponse.result] is *null*.
 *
 * @param comment massage taken from [PolygonResponse.comment].
 */
class ResultExtractingException(comment: String? = null) : PolygonException(comment)
