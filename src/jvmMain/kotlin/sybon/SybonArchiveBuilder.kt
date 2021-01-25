@file:Suppress("BlockingMethodInNonBlockingContext")

package sybon

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
        return Builder(problemId, packageId).build()
    }

    private inner class Builder(
        private val problemId: Int,
        private val packageId: Int
    ) {
        suspend fun build(): Path {
            val (unpackedPath, problem, problemInfo) = coroutineScope {
                val unpackedPath = async { polygonApi.problem.downloadPackage(problemId, packageId) }
                val problem = async { polygonApi.problem.getProblem(problemId) }
                val problemInfo = async { polygonApi.problem.getInfo(problemId).result!! }
                Triple(unpackedPath.await(), problem.await(), problemInfo.await())
            }

            val destinationPath = Paths.get("sybon-packages", "id$problemId-package$packageId", problem.name)

            val checkerPath = destinationPath.resolve("checker")
            val miscPath = destinationPath.resolve("misc")
            val solutionPath = miscPath.resolve("solution")
            val statementPath = destinationPath.resolve("statement")
            val testsPath = destinationPath.resolve("tests")

            for (path in arrayOf(destinationPath, checkerPath, miscPath, solutionPath, statementPath, testsPath)) {
                Files.createDirectories(path)
            }

            val (language, statement) = polygonApi.problem.getStatement(problemId)

            fun writeConfig() {
                val config = """
                    [info]
                    name = ${statement.name}
                    maintainers = ${setOf(problem.owner, "Musin").joinToString(", ")}
                    
                    [resource_limits]
                    time = ${"%.2f".format(problemInfo.timeLimit / 1000.0)}s
                    memory = ${problemInfo.memoryLimit}MiB
                """.trimIndent()
                Files.write(destinationPath.resolve("config.ini"), config.toByteArray())
            }

            fun writeFormat() {
                val text = "bacs/problem/single#simple0"
                Files.write(destinationPath.resolve("format"), text.toByteArray())
            }

            fun writeChecker() {
                Files.write(
                    checkerPath.resolve("check.cpp"),
                    Files.readAllBytes(unpackedPath.resolve("check.cpp"))
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
                val solutionItself = polygonApi.problem.getSolution(problemId, mainSolution.name).bytes()
                Files.write(solutionPath.resolve(mainSolution.name), solutionItself)
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

                val statementItself = polygonApi.problem.getStatementRaw(problemId, packageId, language = language)
                Files.write(statementPath.resolve("problem.pdf"), statementItself)
            }

            suspend fun writeTests() {
                val tests = polygonApi.problem.getTests(problemId).result!!
                val (testInputs, testOutputs) = coroutineScope {
                    Pair(
                        tests.map {
                            async {
                                it.index to polygonApi.problem.getTestInput(problemId, testIndex = it.index)
                            }
                        },
                        tests.map {
                            async {
                                it.index to polygonApi.problem.getTestAnswer(problemId, testIndex = it.index)
                            }
                        }
                    ).let { it.first.awaitAll().toMap() to it.second.awaitAll().toMap() }
                }
                for (test in tests) {
                    Files.write(testsPath.resolve("${test.index}.in"), testInputs[test.index]!!.toByteArray())
                    Files.write(testsPath.resolve("${test.index}.out"), testOutputs[test.index]!!.toByteArray())
                }
            }

            writeConfig()
            writeFormat()
            writeChecker()
            writeSolution()
            writeStatement()
            writeTests()

            val par = destinationPath.parent
            val zipPath = par.resolve("${problem.name}-package${packageId}.zip")
            par.toZipArchive(zipPath)
            return zipPath
        }
    }
}
