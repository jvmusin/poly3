package polygon

import ir.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import polygon.api.*
import polygon.converter.PolygonSourceTypeToIRLanguageConverter
import polygon.converter.PolygonTagToIRVerdictConverter
import polygon.exception.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.notExists
import kotlin.io.path.readBytes

/**
 * Polygon service.
 *
 * Used to communicate to the Polygon API.
 */
interface PolygonService {
    /**
     * Downloads the problem with the given [problemId].
     *
     * Tests might be skipped by setting [includeTests] to *false*.
     *
     * @param problemId id of the problem to download.
     * @param includeTests if true then the problem tests will also be downloaded.
     * @return The problem with or without tests, depending on [includeTests] parameter.
     * @throws ProblemDownloadingException if the downloading failed.
     */
    suspend fun downloadProblem(problemId: Int, includeTests: Boolean = false): IRProblem

    /**
     * Returns all known problems.
     *
     * @return The list of all known problems.
     */
    suspend fun getProblems(): List<Problem>

    /**
     * Returns problem information for the problem with the given [problemId].
     *
     * @param problemId problem id to download information for.
     * @return Problem information.
     * @throws NoSuchProblemException if the problem is not found or if access to the problem is denied.
     */
    suspend fun getProblemInfo(problemId: Int): ProblemInfo
}

class PolygonServiceImpl(
    private val polygonApi: PolygonApi
) : PolygonService {

    data class FullPackageId(
        val packageId: Int,
        val includeTests: Boolean
    )

    private val cache = ConcurrentHashMap<FullPackageId, IRProblem>()

    override suspend fun downloadProblem(problemId: Int, includeTests: Boolean): IRProblem {
        try {
            return downloadProblemInternal(problemId, includeTests)
        } catch (e: Exception) {
            throw ProblemDownloadingException("Не удалось скачать задачу: ${e.message}", e)
        }
    }

    @OptIn(ExperimentalPathApi::class) // TODO ask why file-level opt-ins don't work with Koin
    private suspend fun downloadProblemInternal(problemId: Int, includeTests: Boolean) = coroutineScope {
        //eagerly check for access
        val problem = polygonApi.getProblem(problemId).apply {
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
                throw NoBuiltPackagesException("У задачи нет собранных пакетов. Соберите пакет")
            }
            if (latestPackage != revision) {
                throw OldBuiltPackageException("Последний собранный для задачи пакет не актуален. Соберите новый")
            }
        }

        val info = async {
            polygonApi.getProblemInfo(problemId).result!!.apply {
                if (interactive) {
                    throw FormatNotSupportedException("Интерактивные задачи не поддерживаются")
                }
            }
        }
        val packageId = polygonApi.getLatestPackageId(problemId)

        val statement = async {
            polygonApi.getStatement(problemId)?.let { (language, statement) ->
                val content = polygonApi.getStatementRaw(problemId, packageId, "pdf", language)
                    ?: throw StatementNotFoundException("Не найдена pdf версия условия")
                IRStatement(statement.name, content.toList())
            } ?: throw StatementNotFoundException("Не найдено условие")
        }

        val checker = async {
            val name = "check.cpp"
            val file = polygonApi.downloadPackage(problemId, packageId).resolve(name)
            if (file.notExists()) {
                throw CheckerNotFoundException(
                    "Не найден чекер '$name'. Другие чекеры не поддерживаются"
                )
            }
            IRChecker(name, file.readBytes().decodeToString())
        }

        // fail fast
        statement.await()
        checker.await()
        info.await()

        cache[FullPackageId(packageId, includeTests)]
            .also { if (it != null) return@coroutineScope it }
        if (!includeTests)
            cache[FullPackageId(packageId, false)]
                .also { if (it != null) return@coroutineScope it }

        val tests = async {
            if (!includeTests) return@async emptyList<IRTest>()
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

        val limits = async { info.await().run { IRLimits(timeLimit, memoryLimit) } }

        IRProblem(
            problem.name,
            problem.owner,
            statement.await(),
            limits.await(),
            tests.await(),
            checker.await(),
            solutions.await()
        ).also { cache[FullPackageId(packageId, includeTests)] = it }
    }

    override suspend fun getProblems() = polygonApi.getProblems().result!!
    override suspend fun getProblemInfo(problemId: Int) = polygonApi.getProblemInfo(problemId).result!!
}