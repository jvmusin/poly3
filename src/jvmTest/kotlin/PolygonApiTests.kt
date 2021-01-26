import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import polygon.buildPolygonApi
import polygon.getStatementRaw
import sybon.SybonArchiveBuilder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Ignore

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Ignore
class PolygonApiTests {
    private val polygonApi = buildPolygonApi()

    @Test
    fun testGetProblems() = runBlocking {
        val res = polygonApi.getProblems().result!!
        println(res.size)
        for (p in res) println(p)
    }

    @Test
    fun testGetPackages() = runBlocking {
        val problemId = 141131
        val result = polygonApi.getPackages(problemId).result!!
        println(result.size)
        result.forEach(::println)
    }

    @Test
    fun testGetFiles() = runBlocking {
        val problemId = 133526
        val allFiles = polygonApi.getFiles(problemId).result!!
        for ((type, files) in allFiles) {
            println(type)
            for (file in files) println(file)
            println("---")
        }
    }

    @Test
    fun testStatements() = runBlocking {
        val problemId = 109779
        val allStatements = polygonApi.getStatements(problemId).result!!
        for ((lang, statement) in allStatements) {
            println(lang)
            println(statement)
        }
    }

    @Test
    fun testStatementResources() = runBlocking {
        val problemId = 109779
        val resources = polygonApi.getStatementResources(problemId).result!!
        for (res in resources) {
            println(res)
        }
    }

    @Test
    fun testPackage() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val archive = polygonApi.getPackage(problemId, packageId)
        Files.write(Paths.get("archive.zip"), archive.bytes())
    }

    @Test
    fun testGetStatementPdf() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val pdf = polygonApi.getStatementRaw(problemId, packageId)
        Files.write(Paths.get("statement.pdf"), pdf)
    }

    @Test
    fun testGetStatementHtml() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val pdf = polygonApi.getStatementRaw(problemId, packageId, "html")
        Files.write(Paths.get("statement.html"), pdf)
    }

    @Test
    fun testCreateArchive() = runBlocking<Unit> {
        val problemId = 144845
        SybonArchiveBuilder(polygonApi).build(problemId)
    }

    @Test
    fun testGetSolutions() = runBlocking {
        val problemId = 106223
        val solutions = polygonApi.getSolutions(problemId).result!!
        println(solutions.size)
        solutions.forEach(::println)
    }

    @Test
    fun testGetTests() = runBlocking {
        val problemId = 106223
        val result = polygonApi.getTests(problemId).result!!
        println(result.size)
        result.forEach(::println)
    }
}