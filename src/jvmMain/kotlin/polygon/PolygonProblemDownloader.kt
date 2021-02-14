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
                throw PolygonProblemDownloaderException("Нет доступа на запись. Дайте WRITE доступ пользователю Musin")
            }
            if (modified) {
                throw PolygonProblemDownloaderException(
                    "Файлы задачи изменены. Сначала откатите изменения или закоммитьте их и соберите новый пакет " +
                            "(скорее всего (99.9%) косяк Рустама)"
                )
            }
            if (latestPackage == null) {
                throw PolygonProblemDownloaderException("У задачи нет собранных пакетов. Соберите пакет")
            }
            if (latestPackage != revision) {
                throw PolygonProblemDownloaderException("Последний собранный пакет для задачи не актуален. Соберите новый")
            }
        }

        val packageId = async {
            polygonApi.getLatestPackage(problemId)!!.id
        }

        val statement = async {
            polygonApi.getStatement(problemId)?.let { (language, statement) ->
                val content = polygonApi.getStatementRaw(problemId, packageId.await(), "pdf", language)
                    ?: throw PolygonProblemDownloaderException("Не найдено pdf версия условия")
                IRStatement(statement.name, content)
            } ?: throw PolygonProblemDownloaderException("Не найдено условие")
        }

        val checker = async {
            val name = "check.cpp"
            val file = polygonApi.downloadPackage(problemId, packageId.await()).resolve(name)
            if (file.notExists())
                throw PolygonProblemDownloaderException(
                    "Не найден чекер $name checker. Другие чекеры не поддерживаются"
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