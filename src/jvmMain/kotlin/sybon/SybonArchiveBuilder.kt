@file:Suppress("BlockingMethodInNonBlockingContext")

package sybon

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import polygon.*
import toZipArchive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SybonArchiveBuilder(
    private val polygonApi: PolygonApi
) {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun build(problemId: Int, packageId: Int): Path {
        return Builder(problemId).build()
    }

    private inner class Builder(
        private val problemId: Int
    ) {
        suspend fun build(): Path = coroutineScope {
            val problem = async { polygonApi.problem.getProblem(problemId) }
            val packageId = async { problem.await().latestPackage!! }
            val unpackedPath = async { polygonApi.problem.downloadPackage(problemId, packageId.await()) }
            val problemInfo = async { polygonApi.problem.getInfo(problemId).result!! }

            val destinationPath = Paths.get("sybon-packages", "id$problemId-package$packageId", problem.await().name)

            val checkerPath = destinationPath.resolve("checker")
            val miscPath = destinationPath.resolve("misc")
            val solutionPath = miscPath.resolve("solution")
            val statementPath = destinationPath.resolve("statement")
            val testsPath = destinationPath.resolve("tests")

            for (path in arrayOf(destinationPath, checkerPath, miscPath, solutionPath, statementPath, testsPath)) {
                Files.createDirectories(path)
            }

            val (language, statement) = polygonApi.problem.getStatement(problemId)

            suspend fun writeConfig() {
                val config = """
                    [info]
                    name = ${statement.name}
                    maintainers = ${setOf(problem.await().owner, "Musin").joinToString(", ")}
                    
                    [resource_limits]
                    time = ${"%.2f".format(problemInfo.await().timeLimit / 1000.0)}s
                    memory = ${problemInfo.await().memoryLimit}MiB
                """.trimIndent()
                Files.write(destinationPath.resolve("config.ini"), config.toByteArray())
            }

            fun writeFormat() {
                val format = "bacs/problem/single#simple0"
                Files.write(destinationPath.resolve("format"), format.toByteArray())
            }

            suspend fun writeChecker() {
                Files.write(
                    checkerPath.resolve("check.cpp"),
                    Files.readAllBytes(unpackedPath.await().resolve("check.cpp"))
                )
                val config = """
                    [build]
                    builder = single
                    source = check.cpp
                    libs = testlib.googlecode.com-0.9.12

                    [utility]
                    call = in_out_hint
                    return = testlib
                """.trimIndent()
                Files.write(checkerPath.resolve("config.ini"), config.toByteArray())
            }

            suspend fun writeSolution() {
                val mainSolution = polygonApi.problem.getSolutions(problemId).result!!.single { it.tag == "MA" }
                val solutionContent = polygonApi.problem.getSolution(problemId, mainSolution.name).bytes()
                Files.write(solutionPath.resolve(mainSolution.name), solutionContent)
            }

            suspend fun writeStatement() {
                val pdfIni = """
                    [info]
                    language = C

                    [build]
                    builder = copy
                    source = problem.pdf
                """.trimIndent()
                Files.write(statementPath.resolve("pdf.ini"), pdfIni.toByteArray())

                val statementContent = polygonApi.problem.getStatementRaw(
                    problemId = problemId, packageId = packageId.await(), language = language
                )
                Files.write(statementPath.resolve("problem.pdf"), statementContent)
            }

            suspend fun writeTests() {
                val tests = polygonApi.problem.getTests(problemId).result!!
                val testInputs = tests.associate {
                    it.index to async { polygonApi.problem.getTestInput(problemId, testIndex = it.index) }
                }
                val testAnswers = tests.associate {
                    it.index to async { polygonApi.problem.getTestAnswer(problemId, testIndex = it.index) }
                }
                for (test in tests) {
                    Files.write(testsPath.resolve("${test.index}.in"), testInputs[test.index]!!.await().toByteArray())
                    Files.write(testsPath.resolve("${test.index}.out"), testAnswers[test.index]!!.await().toByteArray())
                }
            }

            writeConfig()
            writeFormat()
            writeChecker()
            writeSolution()
            writeStatement()
            writeTests()

            val par = destinationPath.parent
            val zipPath = par.resolve("${problem.await().name}-package${packageId}.zip")
            par.toZipArchive(zipPath)
            zipPath
        }
    }
}
