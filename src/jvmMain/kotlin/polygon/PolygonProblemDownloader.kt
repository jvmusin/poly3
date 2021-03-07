package polygon

import ir.IRChecker
import ir.IRLimits
import ir.IRProblem
import ir.IRSolution
import ir.IRStatement
import ir.IRTest
import ir.IRTestGroup
import ir.IRTestGroupPointsPolicy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import polygon.api.PolygonApi
import polygon.api.PolygonTest
import polygon.api.Problem
import polygon.api.ProblemInfo
import polygon.api.downloadPackage
import polygon.api.getLatestPackageId
import polygon.api.getProblem
import polygon.api.getStatement
import polygon.api.getStatementRaw
import polygon.converter.PolygonPointsPolicyConverter
import polygon.converter.PolygonSourceTypeToIRLanguageConverter
import polygon.converter.PolygonTagToIRVerdictConverter
import polygon.exception.downloading.ProblemDownloadingException
import polygon.exception.downloading.format.ProblemModifiedException
import polygon.exception.downloading.format.UnsupportedFormatException
import polygon.exception.downloading.packages.NoPackagesBuiltException
import polygon.exception.downloading.packages.OldBuiltPackageException
import polygon.exception.downloading.resource.CheckerNotFoundException
import polygon.exception.downloading.resource.PdfStatementNotFoundException
import polygon.exception.downloading.resource.StatementNotFoundException
import polygon.exception.downloading.tests.MissingTestGroupException
import polygon.exception.downloading.tests.NonSequentialTestIndicesException
import polygon.exception.downloading.tests.NonSequentialTestsInTestGroupException
import polygon.exception.downloading.tests.SamplesNotFirstException
import polygon.exception.downloading.tests.SamplesNotFormingFirstTestGroupException
import polygon.exception.downloading.tests.points.NonIntegralTestPointsException
import polygon.exception.downloading.tests.points.PointsOnSampleException
import polygon.exception.downloading.tests.points.TestPointsDisabledException
import polygon.exception.response.AccessDeniedException
import polygon.exception.response.NoSuchProblemException
import polygon.exception.response.TestGroupsDisabledException
import util.sequentiallyGroupedBy
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.notExists
import kotlin.io.path.readText

/**
 * Polygon problem downloader
 *
 * Used for downloading problems from Polygon.
 */
interface PolygonProblemDownloader {
    /**
     * Downloads the problem with the given [problemId].
     *
     * Tests might be skipped by setting [includeTests].
     *
     * @param problemId id of the problem to download.
     * @param includeTests if true then the problem tests will also be downloaded.
     * @return The problem with or without tests, depending on [includeTests] parameter.
     * @throws NoSuchProblemException if the problem does not exist.
     * @throws AccessDeniedException if not enough rights to download the problem.
     * @throws ProblemDownloadingException if something gone wrong while downloading the problem.
     */
    suspend fun downloadProblem(problemId: Int, includeTests: Boolean): IRProblem
}

class PolygonProblemDownloaderImpl(
    private val polygonApi: PolygonApi
) : PolygonProblemDownloader {

    /**
     * Full package id.
     *
     * Used as a key for the cache of problems.
     *
     * @property packageId Id of problem's package.
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
     * @throws UnsupportedFormatException if the problem has unsupported format.
     */
    private suspend fun getProblemInfo(problemId: Int): ProblemInfo {
        return polygonApi.getProblemInfo(problemId).extract().apply {
            if (interactive) {
                throw UnsupportedFormatException("Интерактивные задачи не поддерживаются")
            }
        }
    }

    /**
     * Returns problem statement.
     *
     * @param problemId id of the problem.
     * @param packageId id of the problem package.
     * @return Problem statement.
     * @throws StatementNotFoundException if there are no statements for the problem.
     * @throws PdfStatementNotFoundException if there are no *.pdf* statements for the problem.
     */
    private suspend fun downloadStatement(problemId: Int, packageId: Int): IRStatement {
        return polygonApi.getStatement(problemId)?.let { (language, statement) ->
            val content = polygonApi.getStatementRaw(problemId, packageId, "pdf", language)
                ?: throw PdfStatementNotFoundException("Не найдена pdf версия условия")
            IRStatement(statement.name, content.toList())
        } ?: throw StatementNotFoundException("Не найдено условие")
    }

    /**
     * Returns problem checker.
     *
     * @param problemId id of the problem.
     * @param packageId if of the problem package.
     * @return Problem checker.
     * @throws CheckerNotFoundException if checker is not found or is not in *.cpp* format.
     */
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

    /**
     * Returns problem solutions.
     *
     * @param problemId id of the problem.
     * @return Problem solutions.
     */
    private suspend fun getSolutions(problemId: Int): List<IRSolution> {
        return polygonApi.getSolutions(problemId).extract().map { solution ->
            IRSolution(
                name = solution.name,
                verdict = PolygonTagToIRVerdictConverter.convert(solution.tag),
                isMain = solution.tag == "MA",
                language = PolygonSourceTypeToIRLanguageConverter.convert(solution.sourceType),
                content = polygonApi.getSolutionContent(problemId, solution.name)
            )
        }
    }

    /**
     * Validates tests and test groups.
     *
     * Test indices should go from **1** to **number of tests**.
     * If it's not so, then [NonSequentialTestIndicesException] is thrown.
     *
     * Samples should go before ordinal tests.
     * If it's not so, [SamplesNotFirstException] is thrown.
     *
     * When test groups are enabled, then all tests should have test group.
     * If it's not so, then [MissingTestGroupException] is thrown.
     *
     * All tests within the same test group should go one-after-another.
     * There should be no two tests from the same test group
     * when there is a test with some other test group between them.
     * If it's not so, then [NonSequentialTestsInTestGroupException] is thrown.
     *
     * Samples should form the first test group.
     * If it's not so, then [SamplesNotFormingFirstTestGroupException] is thrown.
     *
     * @param tests tests for the problem.
     * @throws NonSequentialTestIndicesException if test indices don't go from 1 to its count.
     * @throws SamplesNotFirstException if samples don't go first.
     * @throws MissingTestGroupException if some tests have test groups and some don't (if test groups enabled).
     * @throws NonSequentialTestsInTestGroupException if test groups don't go sequentially (if test groups enabled).
     * @throws SamplesNotFormingFirstTestGroupException if samples don't form the first test group (if test groups enabled).
     */
    private fun validateTests(tests: List<PolygonTest>, testGroupsEnabled: Boolean) {
        if (tests.withIndex().any { (index, test) -> index + 1 != test.index }) {
            throw NonSequentialTestIndicesException("Номера тестов должны идти от 1 до их количества")
        }

        val samples = tests.filter { it.useInStatements }
        val anySamples = samples.any()

        if (anySamples) {
            if (!tests.first().useInStatements || tests.sequentiallyGroupedBy { it.useInStatements }.size > 2) {
                throw SamplesNotFirstException("Сэмплы должны идти перед всеми остальными тестами")
            }
        }

        if (!testGroupsEnabled) return

        val groups = tests.sequentiallyGroupedBy { it.group }
        if (groups.size != groups.distinctBy { it.key }.size) {
            throw NonSequentialTestsInTestGroupException("Тесты из одной группы должны идти последовательно")
        }
        if (anySamples) {
            if (groups.first().size != samples.size) {
                throw SamplesNotFormingFirstTestGroupException("Сэмплы должны образовывать первую группу тестов")
            }
            if (samples.any { it.points != 0.0 }) {
                // TODO check if we really need to check for null here
                throw PointsOnSampleException("Сэмплы не должны давать баллы")
            }
        }
    }

    /**
     * Returns test groups.
     *
     * If test groups are disabled for the problem, returns *null*.
     *
     * Points for the test group are set iff points policy is set to **COMPLETE_GROUP**.
     * In such case, points for the test group are equal to the sum of points of tests within this test group.
     * Otherwise, points are set to *null*.
     *
     * @param problemId id of the problem.
     * @param rawTests raw Polygon tests.
     * @return Test groups or *null* if test groups are disabled.
     * @throws MissingTestGroupException if test groups are enabled and there are tests without test group set.
     * @throws TestPointsDisabledException if test groups are enabled, but test points are not.
     * @throws NonIntegralTestPointsException if tests have non-integral points.
     */
    private suspend fun getTestGroups(problemId: Int, rawTests: List<PolygonTest>): List<IRTestGroup>? {
        val rawTestGroups = try {
            polygonApi.getTestGroup(problemId).extract()
        } catch (e: TestGroupsDisabledException) {
            return null
        }

        if (rawTests.any { it.group == null }) {
            throw MissingTestGroupException("Группы тестов должны быть установлены на всех тестах")
        }
        if (rawTests.any { it.points == null }) {
            throw TestPointsDisabledException(
                "Если используются группы тестов, то баллы должны быть включены, " +
                    "галочка 'Are test points enabled?' в полигоне"
            )
        }
        if (rawTests.any { it.points != it.points!!.toInt().toDouble() }) {
            throw NonIntegralTestPointsException("Баллы должны быть целочисленными")
        }

        val groups = rawTestGroups.associateBy { it.name }
        return rawTests.sequentiallyGroupedBy { it.group!! }.map { (groupName, tests) ->
            val pointsPolicy = when {
                tests.any { it.useInStatements } -> IRTestGroupPointsPolicy.SAMPLES
                else -> PolygonPointsPolicyConverter.convert(groups[groupName]!!.pointsPolicy)
            }
            val points = when (pointsPolicy) {
                IRTestGroupPointsPolicy.COMPLETE_GROUP -> tests.sumOf { it.points!!.toInt() }
                else -> null
            }
            IRTestGroup(groupName, pointsPolicy, tests.map { it.index }, points)
        }
    }

    /**
     * Returns problem tests and test groups.
     *
     * Tests are validated using [validateTests].
     *
     * If test groups are disabled, then returns *null* test groups.
     *
     * If tests are skipped via *[includeTests] = false*, then returns *null* tests.
     *
     * Points for the test are set iff test groups are enabled
     * and the corresponding test group has **EACH_TEST** points policy.
     *
     * @param problemId id of the problem.
     * @param includeTests whether to include tests or not.
     * @return Pair of problem tests and test groups.
     */
    private suspend fun getTestsAndTestGroups(problemId: Int, includeTests: Boolean) = coroutineScope {
        val rawTests = polygonApi.getTests(problemId).extract().sortedBy { it.index }
        val testGroups = getTestGroups(problemId, rawTests)
        validateTests(rawTests, testGroups != null)

        if (!includeTests) return@coroutineScope null to testGroups

        val testGroupsByName = testGroups?.associateBy { it.name }
        val inputs = rawTests.map { async { polygonApi.getTestInput(problemId, it.index) } }
        val answers = rawTests.map { async { polygonApi.getTestAnswer(problemId, it.index) } }
        val tests = rawTests.indices.map { i ->
            val test = rawTests[i]
            val group = testGroupsByName?.get(test.group)
            val points = group
                ?.takeIf { it.pointsPolicy == IRTestGroupPointsPolicy.EACH_TEST }
                ?.let { test.points!!.toInt() }
            IRTest(test.index, test.useInStatements, inputs[i].await(), answers[i].await(), points, test.group)
        }
        tests to testGroups
    }

    /**
     * Returns problem from the cache or *null* if it's not in the cache.
     *
     * @param packageId id of the problem.
     * @param includeTests whether to include tests or not.
     * @return [IRProblem] instance of the problem or *null* if it's not in the cache.
     */
    private fun getProblemFromCache(packageId: Int, includeTests: Boolean): IRProblem? {
        cache[FullPackageId(packageId, includeTests)].also { if (it != null) return it }
        if (!includeTests) {
            cache[FullPackageId(packageId, true)].also { if (it != null) return it }
        }
        return null
    }

    /**
     * Saves problem into the cache.
     *
     * @param packageId if of the problem.
     * @param includeTests whether to include tests or not.
     * @param problem [IRProblem] instance to save.
     */
    private fun saveProblemToCache(packageId: Int, includeTests: Boolean, problem: IRProblem) {
        cache[FullPackageId(packageId, includeTests)] = problem
    }

    @OptIn(ExperimentalPathApi::class)
    override suspend fun downloadProblem(problemId: Int, includeTests: Boolean) = coroutineScope {
        // eagerly check for access
        val problem = getProblem(problemId)

        val packageId = polygonApi.getLatestPackageId(problemId)

        val cached = getProblemFromCache(packageId, includeTests)
        if (cached != null) return@coroutineScope cached

        val info = async { getProblemInfo(problemId) }
        val statement = async { downloadStatement(problemId, packageId) }
        val checker = async { downloadChecker(problemId, packageId) }

        val testsAndTestGroups = async {
            /*
             * These methods can throw an exception about incorrectly formatted problem,
             * so throw them as soon as possible before downloading tests data.
             */
            run {
                info.await()
                statement.await()
                checker.await()
            }
            getTestsAndTestGroups(problemId, includeTests)
        }

        val solutions = async { getSolutions(problemId) }
        val limits = async { with(info.await()) { IRLimits(timeLimit, memoryLimit) } }

        IRProblem(
            name = problem.name,
            owner = problem.owner,
            statement = statement.await(),
            limits = limits.await(),
            tests = testsAndTestGroups.await().first,
            groups = testsAndTestGroups.await().second,
            checker = checker.await(),
            solutions = solutions.await()
        ).also { saveProblemToCache(packageId, includeTests, it) }
    }
}
