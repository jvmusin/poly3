package polygon.converter

import ir.IRVerdict

object PolygonTagToIRVerdictConverter {
    fun convert(tag: String): IRVerdict {
        return when (tag) {
            "MA", "OK" -> IRVerdict.OK
            "WA" -> IRVerdict.WRONG_ANSWER
            "TL", "TO" -> IRVerdict.TIME_LIMIT_EXCEEDED
            "ML" -> IRVerdict.MEMORY_LIMIT_EXCEEDED
            "PE" -> IRVerdict.PRESENTATION_ERROR
            "RJ", "TM" -> IRVerdict.INCORRECT // TM might mean TL or ML
            else -> IRVerdict.OTHER
        }
    }
}
