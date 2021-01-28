import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import sybon.Collection
import sybon.Problem
import sybon.ResourceLimits
import sybon.SybonApiBuilder
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
class SybonApiTests {
    private val api = SybonApiBuilder().build()

    @Test
    fun testGetCollections() = runBlocking {
        val expected = Collection(
            id = 1,
            name = "Global",
            description = "Only Admins",
            problems = emptyList(),
            problemsCount = -1
        )
        val collections = api.getCollections()
        assertEquals(1, collections.size)
        val collection = collections.first()
        assert(collection.problemsCount >= 7022)
        assertEquals(
            expected,
            collection.copy(problemsCount = -1)
        )
    }

    @Test
    fun testGetCollection() = runBlocking {
        val collectionId = 1
        val expected = Collection(
            id = collectionId,
            name = "Global",
            description = "Only Admins",
            problems = emptyList(),
            problemsCount = 0
        )
        val collection = api.getCollection(collectionId)
        assertEquals(
            expected,
            collection.copy(problems = emptyList(), problemsCount = 0)
        )
        assert(collection.problems.size == collection.problemsCount)
        assert(collection.problems.size >= 7022)
    }

    @Test
    fun testGetProblem() = runBlocking {
        val problemId = 72147
        val expected = Problem(
            id = problemId,
            name = "Лягушка и многоугольник",
            author = "Musin",
            format = "pdf",
            statementUrl = "http://statement.bacs.cs.istu.ru/statement/get/CkhiYWNzL3Byb2JsZW0vbXVuaWNpcGFsMjAyMC05MTEtZnJvZy1hbmQtcG9seWdvbi9zdGF0ZW1lbnQvdmVyc2lvbnMvQy9wZGYSBgoEMc68zg/bacs/RRtTY4-b81yftuSQdorVUh5w7Z8m-bDUtKdT172cGv9dSMFpF95pNdlbElEyfpMPVmgnokw-yaNEJ2tFgPvCYUvrQaxyYdpvMcFc-MklPkxvooZWcdDm3Xvu4MbD8bOmyn1JwzrydffH1vzBs3CaA-AzO89PP4Di1mu1-IScfN4-JDNN4TIe9RqdtJUGKc61XX96Zh7sVmukRBeiUUILcc3Eem3HPGm9xrKDQcxexSM9B0heJxWqVvKbGv11m1ojTdU-fO5Vi1oOif9WCMGU47oCV6upmk57_Fq-HyuQt1b2s5xGyZ1ToFSwicDF4Z9MlqsMPhOPMuWV9KCr4_GC7A",
            collectionId = 1,
            testsCount = 62,
            pretests = emptyList(),
            inputFileName = "STDIN",
            outputFileName = "STDOUT",
            internalProblemId = "municipal2020-911-frog-and-polygon",
            resourceLimits = ResourceLimits(
                timeLimitMillis = 1000,
                memoryLimitBytes = 268435456
            )
        )
        val problem = api.getProblem(problemId)
        assertEquals(expected, problem)
    }

    @Test
    fun testGetProblemStatementUrl() = runBlocking {
        val problemId = 72147
        val expected =
            "http://statement.bacs.cs.istu.ru/statement/get/CkhiYWNzL3Byb2JsZW0vbXVuaWNpcGFsMjAyMC05MTEtZnJvZy1hbmQtcG9seWdvbi9zdGF0ZW1lbnQvdmVyc2lvbnMvQy9wZGYSBgoEMc68zg/bacs/RRtTY4-b81yftuSQdorVUh5w7Z8m-bDUtKdT172cGv9dSMFpF95pNdlbElEyfpMPVmgnokw-yaNEJ2tFgPvCYUvrQaxyYdpvMcFc-MklPkxvooZWcdDm3Xvu4MbD8bOmyn1JwzrydffH1vzBs3CaA-AzO89PP4Di1mu1-IScfN4-JDNN4TIe9RqdtJUGKc61XX96Zh7sVmukRBeiUUILcc3Eem3HPGm9xrKDQcxexSM9B0heJxWqVvKbGv11m1ojTdU-fO5Vi1oOif9WCMGU47oCV6upmk57_Fq-HyuQt1b2s5xGyZ1ToFSwicDF4Z9MlqsMPhOPMuWV9KCr4_GC7A"
        val actual = api.getProblemStatementUrl(problemId)
        assertEquals(expected, actual)
    }
}