package sybon

import api.AdditionalProblemProperties
import ir.IRProblem
import java.nio.file.Path

interface SybonArchiveBuilder {
    suspend fun build(problem: IRProblem, properties: AdditionalProblemProperties): Path
}