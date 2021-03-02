package polygon

/**
 * Polygon API configuration properties.
 *
 * @property url url to the API.
 * @property apiKey apiKey to access the API.
 * @property secret secret key to access the API.
 */
data class PolygonConfig(
    val url: String,
    val apiKey: String,
    val secret: String
)
