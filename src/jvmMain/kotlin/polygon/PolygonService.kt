package polygon

import ir.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import polygon.converter.PolygonSourceTypeToIRLanguageConverter
import polygon.converter.PolygonTagToIRVerdictConverter
import util.extract
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.notExists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

private val packagesCache = ConcurrentHashMap<Int, Path>()

@OptIn(ExperimentalPathApi::class)
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun PolygonApi.downloadPackage(problemId: Int, packageId: Int): Path {
    if (packagesCache.containsKey(packageId)) return packagesCache[packageId]!!
    val destination = Paths.get("polygon-problems").resolve("id$problemId-package$packageId-${UUID.randomUUID()}")
    val archivePath = Files.createTempDirectory("${destination.fileName}-").resolve("archive.zip")
    archivePath.writeBytes(getPackage(problemId, packageId).bytes())
    ZipFile(archivePath.toFile()).use { it.extract(destination) }
    Files.delete(archivePath)
    return destination.also { packagesCache[packageId] = it }
}

@OptIn(ExperimentalPathApi::class)
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun PolygonApi.getStatementRaw(
    problemId: Int,
    packageId: Int,
    type: String = "pdf",
    language: String = "russian"
): ByteArray? {
    val filePath = downloadPackage(problemId, packageId)
        .resolve("statements")
        .resolve(".$type")
        .resolve(language)
        .resolve("problem.$type")
    if (filePath.notExists()) return null
    return filePath.readBytes()
}

private suspend fun PolygonApi.getProblem(problemId: Int): Problem {
    return getProblems().result!!.single { it.id == problemId }
}

private suspend fun PolygonApi.getStatement(problemId: Int, language: String? = null): Pair<String, Statement>? {
    return getStatements(problemId).result!!.entries.firstOrNull {
        language == null || it.key == language
    }?.let { it.key to it.value }
}

private suspend fun PolygonApi.getLatestPackageId(problemId: Int): Int {
    return getPackages(problemId).result!!
        .filter { it.state == Package.State.READY }
        .maxOf { it.id }
}

interface PolygonService {
    suspend fun downloadProblem(problemId: Int, onlyEssentials: Boolean = false): IRProblem
    suspend fun getProblems(): List<Problem>
    suspend fun getProblemInfo(problemId: Int): ProblemInfo
}

class PolygonServiceImpl(
    private val polygonApi: PolygonApi
) : PolygonService {

    data class FullPackageId(
        val packageId: Int,
        val onlyEssentials: Boolean
    )

    private val cache = ConcurrentHashMap<FullPackageId, IRProblem>()

    @OptIn(ExperimentalPathApi::class) // TODO ask why file-level opt-ins don't work with Koin
    override suspend fun downloadProblem(problemId: Int, onlyEssentials: Boolean) = coroutineScope {
        //eagerly check for access
        val problem = polygonApi.getProblem(problemId).apply {
            if (accessType == Problem.AccessType.READ) {
                throw PolygonProblemDownloadException("Нет доступа на запись. Дайте WRITE доступ пользователю Musin")
            }
            if (modified) {
                throw PolygonProblemDownloadException(
                    "Файлы задачи изменены. Сначала откатите изменения или закоммитьте их и соберите новый пакет " +
                            "(скорее всего (99.9%) косяк Рустама)"
                )
            }
            if (latestPackage == null) {
                throw PolygonProblemDownloadException("У задачи нет собранных пакетов. Соберите пакет")
            }
            if (latestPackage != revision) {
                throw PolygonProblemDownloadException("Последний собранный для задачи пакет не актуален. Соберите новый")
            }
        }

        val info = async {
            polygonApi.getProblemInfo(problemId).result!!.apply {
                if (interactive) {
                    throw PolygonProblemDownloadException("Интерактивные задачи не поддерживаются")
                }
            }
        }
        val packageId = polygonApi.getLatestPackageId(problemId)

        val statement = async {
            polygonApi.getStatement(problemId)?.let { (language, statement) ->
                val content = polygonApi.getStatementRaw(problemId, packageId, "pdf", language)
                    ?: throw PolygonProblemDownloadException("Не найдена pdf версия условия")
                IRStatement(statement.name, content)
            } ?: throw PolygonProblemDownloadException("Не найдено условие")
        }

        val checker = async {
            val name = "check.cpp"
            val file = polygonApi.downloadPackage(problemId, packageId).resolve(name)
            if (file.notExists()) {
                throw PolygonProblemDownloadException(
                    "Не найден чекер '$name'. Другие чекеры не поддерживаются"
                )
            }
            IRChecker(name, file.readBytes().decodeToString())
        }

        // fail fast
        statement.await()
        checker.await()
        info.await()

        cache[FullPackageId(packageId, onlyEssentials)]
            .also { if (it != null) return@coroutineScope it }
        if (onlyEssentials)
            cache[FullPackageId(packageId, false)]
                .also { if (it != null) return@coroutineScope it }

        val tests = async {
            if (onlyEssentials) return@async emptyList<IRTest>()
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

        val limits = info.await().run { IRLimits(timeLimit, memoryLimit) }

        IRProblem(
            problem.name,
            problem.owner,
            statement.await(),
            limits,
            tests.await(),
            checker.await(),
            solutions.await()
        ).also { cache[FullPackageId(packageId, onlyEssentials)] = it }
    }

    override suspend fun getProblems() = polygonApi.getProblems().result!!
    override suspend fun getProblemInfo(problemId: Int) = polygonApi.getProblemInfo(problemId).result!!
}