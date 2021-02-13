@file:OptIn(ExperimentalPathApi::class)

package polygon

import ir.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.notExists
import kotlin.io.path.readBytes

class PolygonProblemDownloader(private val polygonApi: PolygonApi) {
    suspend fun download(problemId: Int, skipTests: Boolean = false) = coroutineScope {
        //eagerly check for access
        val problem = polygonApi.getProblem(problemId).apply {
            if (accessType == Problem.AccessType.READ) {
                throw PolygonProblemDownloaderException("No WRITE access (only READ)")
            }
            if (modified) {
                throw PolygonProblemDownloaderException(
                    "Problem is modified. Commit changes and build a new package or discard the changes"
                )
            }
            if (latestPackage == null) {
                throw PolygonProblemDownloaderException("Problem has no build packages")
            }
            if (latestPackage != revision) {
                throw PolygonProblemDownloaderException("Problem doesn't have package for the latest revision. Build the package to fix it")
            }
        }

        val packageId = async {
            polygonApi.getLatestPackage(problemId)?.id
                ?: throw PolygonProblemDownloaderException("Problem has no build packages")
        }

        val statement = async {
            polygonApi.getStatement(problemId)?.let { (language, statement) ->
                val content = polygonApi.getStatementRaw(problemId, packageId.await(), "pdf", language)
                    ?: throw PolygonProblemDownloaderException(
                        "There is no pdf version of the statement ${statement.name} in $language language"
                    )
                IRStatement(statement.name, content)
            } ?: throw PolygonProblemDownloaderException("There are no statements for problem $problemId")
        }

        val checker = async {
            val name = "check.cpp"
            val file = polygonApi.downloadPackage(problemId, packageId.await()).resolve(name)
            if (file.notExists())
                throw PolygonProblemDownloaderException(
                    "There is no $name checker. Other kinds of checkers are not supported"
                )
            IRChecker(name, file.readBytes().decodeToString())
        }

        // fail fast before downloading tests
        packageId.await()
        statement.await()
        checker.await()

        val tests = async {
            if (skipTests) return@async emptyList<IRTest>()
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

        val limits = async {
            polygonApi.getInfo(problemId).result!!.run { IRLimits(timeLimit, memoryLimit) }
        }

        IRProblem(
            problem.name,
            problem.owner,
            statement.await(),
            limits.await(),
            tests.await(),
            checker.await(),
            solutions.await()
        )
    }
}