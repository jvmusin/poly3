@file:Suppress("BlockingMethodInNonBlockingContext")

package sybon

import getLogger
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
    suspend fun build(problemId: Int): Path {
        try {
            return buildInternal(problemId)
        } catch (ex: SybonArchiveBuildException) {
            throw ex
        } catch (ex: Exception) {
            throw SybonArchiveBuildException(ex)
        }
    }

    private suspend fun buildInternal(problemId: Int): Path = coroutineScope {
        val problem = async { polygonApi.getProblem(problemId) }
        val packageId = async { polygonApi.getLatestPackage(problemId)!!.id }
        val unpackedPath = async { polygonApi.downloadPackage(problemId, packageId.await()) }
        val problemInfo = async { polygonApi.getInfo(problemId).result!! }

        val destinationPath = Paths.get(
            "sybon-packages",
            "id$problemId-package${packageId.await()}",
            problem.await().name
        )

        val checkerPath = destinationPath.resolve("checker")
        val miscPath = destinationPath.resolve("misc")
        val solutionPath = miscPath.resolve("solution")
        val statementPath = destinationPath.resolve("statement")
        val testsPath = destinationPath.resolve("tests")

        for (path in arrayOf(destinationPath, checkerPath, miscPath, solutionPath, statementPath, testsPath)) {
            Files.createDirectories(path)
        }

        val (language, statement) = polygonApi.getStatement(problemId)

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
            val checkerFilePath = unpackedPath.await().resolve("check.cpp")
            if (Files.notExists(checkerFilePath)) {
                throw SybonArchiveBuildException(
                    "" +
                            "Checker \"check.cpp\" doesn't exist in the polygon problem package. " +
                            "Other kinds of checkers are not supported."
                )
            }
            Files.write(checkerPath.resolve("check.cpp"), Files.readAllBytes(checkerFilePath))
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
            val mainSolution =
                polygonApi.getSolutions(problemId).result!!.single { it.tag == "MA" }
            val solutionContent = polygonApi.getSolution(problemId, mainSolution.name).bytes()
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

            val statementContent = polygonApi.getStatementRaw(
                problemId = problemId, packageId = packageId.await(), language = language
            ) ?: throw SybonArchiveBuildException(
                "Problem doesn't have pdf statements. Add pdf or rebuild package (sometimes it helps)."
            )
            Files.write(statementPath.resolve("problem.pdf"), statementContent)
        }

        suspend fun writeTests() {
            try {
                val tests = polygonApi.getTests(problemId).result!!
                fun writeTest(index: Int, type: String, content: String) {
                    Files.write(
                        testsPath.resolve("${index}.$type"),
                        content.toByteArray()
                    )
                }

                val inputs = tests.map {
                    async {
                        writeTest(it.index, "in", polygonApi.getTestInput(problemId, testIndex = it.index))
                    }
                }
                val outputs = tests.map {
                    async {
                        writeTest(it.index, "out", polygonApi.getTestAnswer(problemId, testIndex = it.index))
                    }
                }
                inputs.awaitAll()
                outputs.awaitAll()
            } catch (ex: Exception) {
                throw SybonArchiveBuildException("Failed to load test data: ${ex.message}", ex)
            }
        }

        writeConfig()
        writeFormat()
        writeChecker()
        writeSolution()
        writeStatement()
        writeTests()

        val actualPackageId = polygonApi.getLatestPackage(problemId)!!.id
        if (actualPackageId != packageId.await()) {
            getLogger(javaClass).info(
                "" +
                        "New package was created for problem ${problem.await()}. " +
                        "Old package id was ${packageId.await()}, actual is $actualPackageId. " +
                        "Rebuilding the sybon package."
            )
            return@coroutineScope buildInternal(problemId)
        }

        val par = destinationPath.parent
        val zipPath = par.resolve("${problem.await().name}-package${packageId.await()}.zip")
        par.toZipArchive(zipPath)
        zipPath
    }
}
