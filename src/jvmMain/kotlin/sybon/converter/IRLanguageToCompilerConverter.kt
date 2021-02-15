package sybon.converter

import ir.IRLanguage
import sybon.api.SybonCompiler
import sybon.SybonCompilers

object IRLanguageToCompilerConverter {
    private fun convert(language: IRLanguage): SybonCompiler? {
        return when (language) {
            IRLanguage.CPP -> SybonCompilers.CPP
            IRLanguage.JAVA -> SybonCompilers.JAVA
            IRLanguage.PYTHON2 -> SybonCompilers.PYTHON2
            IRLanguage.PYTHON3 -> SybonCompilers.PYTHON3
            else -> null
        }
    }

    fun IRLanguage.toSybonCompiler() = convert(this)
}