import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import polygon.Package
import polygon.buildPolygonApi
import polygon.getStatementRaw
import sybon.SybonArchiveBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PolygonApiTests {
    private val today = LocalDate.of(2021, 1, 24)
    private val todaySeconds = today.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
    private val polygonApi = buildPolygonApi()

    @Test
    fun testProblems() = runBlocking {
        val res = polygonApi.problem.getProblems().result!!
        println(res.size)
        for (p in res) println(p)
    }

    @Test
    fun testGetPackages() = runBlocking {
        val problemId = 155265
        val result = polygonApi.problem.getPackages(problemId)
        println(result)
    }

    @Test
    fun testFiles() = runBlocking {
        val problemId = 109779
        val allFiles = polygonApi.problem.getFiles(problemId).result!!
        for ((type, files) in allFiles) {
            println(type)
            for (file in files) println(file)
            println("---")
        }
    }

    @Test
    fun testStatements() = runBlocking {
        val problemId = 109779
        val allStatements = polygonApi.problem.getStatements(problemId).result!!
        for ((lang, statement) in allStatements) {
            println(lang)
            println(statement)
        }
    }

    @Test
    fun testStatementResources() = runBlocking {
        val problemId = 109779
        val resources = polygonApi.problem.getStatementResources(problemId).result!!
        for (res in resources) {
            println(res)
        }
    }

    @Test
    fun testPackage() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val archive = polygonApi.problem.getPackage(problemId, packageId)
        Files.write(Paths.get("archive.zip"), archive.bytes())
    }

    @Test
    fun testGetStatementPdf() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val pdf = polygonApi.problem.getStatementRaw(problemId, packageId)
        Files.write(Paths.get("statement.pdf"), pdf)
    }

    @Test
    fun testGetStatementHtml() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val pdf = polygonApi.problem.getStatementRaw(problemId, packageId, "html")
        Files.write(Paths.get("statement.html"), pdf)
    }

    @Test
    fun testCreateArchive() = runBlocking<Unit> {
        val problemId = 141639
        val packageId = 357125
        SybonArchiveBuilder(polygonApi).build(problemId, packageId)
    }

    @Test
    fun testGetSolutions() = runBlocking<Unit> {
        val problemId = 106223
        val solutions = polygonApi.problem.getSolutions(problemId).result!!
        println(solutions.size)
        solutions.forEach(::println)
    }

    @Test
    fun testGetTests() = runBlocking<Unit> {
        val problemId = 106223
        val result = polygonApi.problem.getTests(problemId).result!!
        println(result.size)
        result.forEach(::println)
    }
}