@file:OptIn(ExperimentalPathApi::class)

package polygon

import ir.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readBytes

class PolygonProblemDownloader(private val polygonApi: PolygonApi) {
    suspend fun download(problemId: Int) = coroutineScope {
        val problem = async { polygonApi.getProblem(problemId) }
        val packageId = async { polygonApi.getLatestPackage(problemId)!!.id }

        val tests = async {
            val tests = polygonApi.getTests(problemId).result!!.sortedBy { it.index }
            val inputs = tests.map { async { polygonApi.getTestInput(problemId, it.index) } }
            val answers = tests.map { async { polygonApi.getTestAnswer(problemId, it.index) } }
            val ins = inputs.awaitAll()
            val outs = answers.awaitAll()
            tests.indices.map { i ->
                val test = tests[i]
                IRTest(test.index, test.useInStatements, ins[i], outs[i])
            }
        }

        val solutions = async {
            polygonApi.getSolutions(problemId).result!!.map { solution ->
                val content = polygonApi.getSolutionContent(problemId, solution.name).bytes().decodeToString()
                IRSolution(solution.name, solution.tag, solution.sourceType, content)
            }
        }

        val statement = async {
            polygonApi.getStatement(problemId).let { (language, statement) ->
                val content = polygonApi.getStatementRaw(problemId, packageId.await(), "pdf", language)!!
                IRStatement(statement.name, content)
            }
        }

        val checker = async {
            val name = "check.cpp"
            val content = polygonApi.downloadPackage(problemId, packageId.await())
                .resolve(name)
                .readBytes()
                .decodeToString()
            IRChecker(name, content)
        }

        val limits = async {
            polygonApi.getInfo(problemId).result!!.run { IRLimits(timeLimit, memoryLimit) }
        }

        IRProblem(
            problem.await().name,
            problem.await().owner,
            statement.await(),
            limits.await(),
            tests.await(),
            checker.await(),
            solutions.await()
        )
    }
}