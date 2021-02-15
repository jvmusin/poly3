package polygon.converter

import ir.IRLanguage

object PolygonSourceTypeToIRLanguageConverter {
    fun convert(sourceType: String): IRLanguage {
        return when(sourceType) {
            "cpp.g++17" -> IRLanguage.CPP
            "kotlin" -> IRLanguage.KOTLIN
            "python.2" -> IRLanguage.PYTHON2
            "python.3" -> IRLanguage.PYTHON3
            "java8" -> IRLanguage.JAVA
            else -> IRLanguage.OTHER
        }
    }
}