import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import polygon.*
import sybon.SybonArchiveBuildException
import sybon.SybonArchiveBuilder
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.Ignore

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Ignore
class PolygonApiTests {
    private val api = PolygonApiBuilder().build()

    @Test
    fun testGetProblems() = runBlocking {
        val result = api.getProblems().result!!
        println(result.size)
        result.forEach(::println)
    }

    @Test
    fun testGetPackages() = runBlocking {
        val problemId = 141131
        val result = api.getPackages(problemId).result!!
        println(result.size)
        result.forEach(::println)
    }

    @Test
    fun testGetFiles() = runBlocking {
        val problemId = 133526
        val allFiles = api.getFiles(problemId).result!!
        for ((type, files) in allFiles) {
            println(type)
            for (file in files) println(file)
            println("---")
        }
    }

    @Test
    fun testStatements() = runBlocking {
        val problemId = 109779
        val allStatements = api.getStatements(problemId).result!!
        for ((lang, statement) in allStatements) {
            println(lang)
            println(statement)
        }
    }

    @Test
    fun testStatementResources() = runBlocking {
        val problemId = 109779
        val resources = api.getStatementResources(problemId).result!!
        resources.forEach(::println)
    }

    @Test
    fun testGetPackage() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val archive = api.getPackage(problemId, packageId)
        Files.write(Paths.get("archive.zip"), archive.bytes())
    }

    @Test
    fun testBuildArchive() = runBlocking<Unit> {
        val builder = SybonArchiveBuilder(api)
        val problemId = 92201
        builder.build(problemId)
    }

    @Test
    fun testDownloadAllPackages() = runBlocking<Unit> {
        val problems = api.getProblems().result!!.filter { it.accessType != Problem.AccessType.READ }
        problems.map {
            async {
                val latestPackage = api.getLatestPackage(it.id)
                try {
                    if (latestPackage != null)
                        api.downloadPackage(it.id, latestPackage.id)
                    else Paths.get("")
                } catch (ex: SybonArchiveBuildException) {
                    getLogger(javaClass).info("Package for problem $it not downloaded: ${ex.message}")
                    Paths.get("")
                }
            }
        }.awaitAll()
    }

    @Test
    fun testBuildAllSybonArchives() = runBlocking<Unit> {
        coroutineScope {
            val builder = SybonArchiveBuilder(api)
            val problems = api.getProblems().result!!.filter { it.accessType != Problem.AccessType.READ }
            problems.map {
                async {
                    val latestPackage = api.getLatestPackage(it.id)
                    try {
                        if (latestPackage != null)
                            builder.build(it.id)
                        else Paths.get("")
                    } catch (ex: SybonArchiveBuildException) {
                        getLogger(javaClass).info("QQQQQ Package for problem $it not built: ${ex.message}")
                        Paths.get("")
                    }
                }
            }.awaitAll()
        }
    }

    @Test
    fun testGetStatementPdf() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val pdf = api.getStatementRaw(problemId, packageId)
        Files.write(Paths.get("statement.pdf"), pdf)
    }

    @Test
    fun testGetStatementHtml() = runBlocking<Unit> {
        val problemId = 144543
        val packageId = 393239
        val pdf = api.getStatementRaw(problemId, packageId, "html")
        Files.write(Paths.get("statement.html"), pdf)
    }

    @Test
    fun testCreateArchive() = runBlocking<Unit> {
        val problemId = 144845
        SybonArchiveBuilder(api).build(problemId)
    }

    @Test
    fun testGetSolutions() = runBlocking {
        val problemId = 106223
        val solutions = api.getSolutions(problemId).result!!
        println(solutions.size)
        solutions.forEach(::println)
    }

    @Test
    fun testGetTests() = runBlocking {
        val problemId = 106223
        val result = api.getTests(problemId).result!!
        println(result.size)
        result.forEach(::println)
    }
}