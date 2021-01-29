package sybon

data class SybonArchiveProperties(
    val addPrefix: String? = null,
    val addSuffix: String? = null,
    val timeLimitMillis: Int? = null,
    val memoryLimitBytes: Int? = null
)