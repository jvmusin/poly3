package polygon

import ir.IRChecker
import ir.IRLimits
import ir.IRProblem
import ir.IRSolution
import ir.IRStatement
import ir.IRTest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import polygon.api.PolygonApi
import polygon.api.Problem
import polygon.api.ProblemInfo
import polygon.api.downloadPackage
import polygon.api.getLatestPackageId
import polygon.api.getProblem
import polygon.api.getStatement
import polygon.api.getStatementRaw
import polygon.converter.PolygonSourceTypeToIRLanguageConverter
import polygon.converter.PolygonTagToIRVerdictConverter
import polygon.exception.downloading.format.ProblemModifiedException
import polygon.exception.downloading.format.UnsupportedProblemFormatException
import polygon.exception.downloading.packages.NoPackagesBuiltException
import polygon.exception.downloading.packages.OldBuiltPackageException
import polygon.exception.downloading.resource.CheckerNotFoundException
import polygon.exception.downloading.resource.PdfStatementNotFoundException
import polygon.exception.downloading.resource.StatementNotFoundException
import polygon.exception.response.AccessDeniedException
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.notExists
import kotlin.io.path.readText

/**
 * Polygon problem downloader
 *
 * Used for downloading problems from Polygon.
 *
 * @property polygonApi Polygon API.
 */
class PolygonProblemDownloader(
    private val polygonApi: PolygonApi
) {

    /**
     * Full package id.
     *
     * Used as a key for the cache of problems.
     *
     * @property packageId if of problem's package.
     * @property includeTests Includes tests or not.
     */
    private data class FullPackageId(
        val packageId: Int,
        val includeTests: Boolean
    )

    /**
     * Problems cache.
     */
    private val cache = ConcurrentHashMap<FullPackageId, IRProblem>()

    /**
     * Returns problem using Polygon API.
     *
     * @param problemId id of the problem.
     * @return Problem with given [problemId] from Polygon API.
     * @throws AccessDeniedException if no WRITE access is given.
     * @throws ProblemModifiedException if the problem has uncommitted changes.
     * @throws NoPackagesBuiltException if the problem has no built packages.
     * @throws OldBuiltPackageException if latest built package for the problem is not for the latest revision.
     */
    private suspend fun getProblem(problemId: Int): Problem {
        return polygonApi.getProblem(problemId).apply {
            if (accessType == Problem.AccessType.READ) {
                throw AccessDeniedException("Нет доступа на запись. Дайте WRITE доступ пользователю Musin")
            }
            if (modified) {
                throw ProblemModifiedException(
                    "Файлы задачи изменены. Сначала откатите изменения или закоммитьте их и соберите новый пакет " +
                        "(скорее всего (99.9%) косяк Рустама)"
                )
            }
            if (latestPackage == null) {
                throw NoPackagesBuiltException("У задачи нет собранных пакетов. Соберите пакет")
            }
            if (latestPackage != revision) {
                throw OldBuiltPackageException("Последний собранный для задачи пакет не актуален. Соберите новый")
            }
        }
    }

    /**
     * Returns problem info from Polygon API.
     *
     * @param problemId id of the problem.
     * @return Problem info for the problem with given [problemId] from Polygon API.
     * @throws UnsupportedProblemFormatException if the problem has unsupported format.
     */
    private suspend fun getProblemInfo(problemId: Int): ProblemInfo {
        return polygonApi.getProblemInfo(problemId).extract().apply {
            if (interactive) {
                throw UnsupportedProblemFormatException("Интерактивные задачи не поддерживаются")
            }
        }
    }

    private suspend fun downloadStatement(problemId: Int, packageId: Int): IRStatement {
        return polygonApi.getStatement(problemId)?.let { (language, statement) ->
            val content = polygonApi.getStatementRaw(problemId, packageId, "pdf", language)
                ?: throw PdfStatementNotFoundException("Не найдена pdf версия условия")
            IRStatement(statement.name, content.toList())
        } ?: throw StatementNotFoundException("Не найдено условие")
    }

    @OptIn(ExperimentalPathApi::class)
    private suspend fun downloadChecker(problemId: Int, packageId: Int): IRChecker {
        val name = "check.cpp"
        val file = polygonApi.downloadPackage(problemId, packageId).resolve(name)
        if (file.notExists()) {
            throw CheckerNotFoundException(
                "Не найден чекер '$name'. Другие чекеры не поддерживаются"
            )
        }
        return IRChecker(name, file.readText())
    }

    private suspend fun getTests(problemId: Int) = coroutineScope {
        val tests = polygonApi.getTests(problemId).extract().sortedBy { it.index }
        val inputs = tests.map { async { polygonApi.getTestInput(problemId, it.index) } }
        val answers = tests.map { async { polygonApi.getTestAnswer(problemId, it.index) } }
        val ins = inputs.awaitAll()
        val outs = answers.awaitAll()
        tests.indices.map { i ->
            val test = tests[i]
            IRTest(test.index, test.useInStatements, ins[i], outs[i])
        }
    }

    private suspend fun getSolutions(problemId: Int): List<IRSolution> {
        return polygonApi.getSolutions(problemId).extract().map { solution ->
            val content = polygonApi.getSolutionContent(problemId, solution.name).use {
                it.bytes().decodeToString()
            }
            IRSolution(
                name = solution.name,
                verdict = PolygonTagToIRVerdictConverter.convert(solution.tag),
                isMain = solution.tag == "MA",
                language = PolygonSourceTypeToIRLanguageConverter.convert(solution.sourceType),
                content = content
            )
        }
    }

    private fun getProblemFromCache(packageId: Int, includeTests: Boolean): IRProblem? {
        cache[FullPackageId(packageId, includeTests)].also { if (it != null) return it }
        if (!includeTests) {
            cache[FullPackageId(packageId, true)].also { if (it != null) return it }
        }
        return null
    }

    private fun putProblemToCache(packageId: Int, includeTests: Boolean, problem: IRProblem) {
        cache[FullPackageId(packageId, includeTests)] = problem
    }

    @OptIn(ExperimentalPathApi::class)
    suspend fun downloadProblem(problemId: Int, includeTests: Boolean) = coroutineScope {
        // eagerly check for access
        val problem = getProblem(problemId)

        val info = async { getProblemInfo(problemId) }
        val packageId = async { polygonApi.getLatestPackageId(problemId) }
        val statement = async { downloadStatement(problemId, packageId.await()) }
        val checker = async { downloadChecker(problemId, packageId.await()) }

        /*
         * Only these methods can throw an exception about incorrectly formatted problem,
         * so throw them as soon as possible.
         */
        statement.await()
        checker.await()
        info.await()

        val cached = getProblemFromCache(packageId.await(), includeTests)
        if (cached != null) return@coroutineScope cached

        val tests = async { if (!includeTests) emptyList() else getTests(problemId) }
        val solutions = async { getSolutions(problemId) }
        val limits = async { with(info.await()) { IRLimits(timeLimit, memoryLimit) } }

        IRProblem(
            name = problem.name,
            owner = problem.owner,
            statement = statement.await(),
            limits = limits.await(),
            tests = tests.await(),
            checker = checker.await(),
            solutions = solutions.await()
        ).also { putProblemToCache(packageId.await(), includeTests, it) }
    }
}