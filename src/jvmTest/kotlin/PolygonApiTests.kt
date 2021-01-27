import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import polygon.*
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
        val result = polygonApi.getProblems().result!!
        println(result.size)
        result.forEach(::println)
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
        resources.forEach(::println)
    }

    @Test
    fun testGetPackage() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val archive = polygonApi.getPackage(problemId, packageId)
        Files.write(Paths.get("archive.zip"), archive.bytes())
    }

    @Test
    fun testDownloadAllPackages() {
        runBlocking {
            val problems = polygonApi.getProblems().result!!.filter { it.accessType != Problem.AccessType.READ }
            problems.map {
                async {
                    val latestPackage = polygonApi.getLatestPackage(it.id)
                    if (latestPackage != null)
                        polygonApi.downloadPackage(it.id, latestPackage.id)
                    else Paths.get("")
                }
            }.awaitAll()
        }
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