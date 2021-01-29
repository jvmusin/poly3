@file:Suppress("BlockingMethodInNonBlockingContext")

package sybon

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import polygon.*
import util.getLogger
import util.toZipArchive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class SybonArchiveBuilder(
    private val polygonApi: PolygonApi
) {
    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun build(problemId: Int, properties: SybonArchiveProperties): Path {
        try {
            return buildInternal(problemId, properties)
        } catch (ex: SybonArchiveBuildException) {
            throw ex
        } catch (ex: Exception) {
            throw SybonArchiveBuildException(ex)
        }
    }

    private suspend fun buildInternal(problemId: Int, properties: SybonArchiveProperties): Path = coroutineScope {
        val problem = async { polygonApi.getProblem(problemId) }
        val packageId = async { polygonApi.getLatestPackage(problemId)!!.id }
        val unpackedPath = async { polygonApi.downloadPackage(problemId, packageId.await()) }
        val problemInfo = async { polygonApi.getInfo(problemId).result!! }

        val destinationPath = Paths.get(
            "sybon-packages",
            "id$problemId-rev${problem.await().revision}",
            "${properties.addPrefix.orEmpty()}${problem.await().name}${properties.addSuffix.orEmpty()}"
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

        suspend fun writeConfig(sampleTests: List<PolygonTest>) {
            val config = """
                [info]
                name = ${statement.name}
                maintainers = ${setOf(problem.await().owner, "Musin").joinToString(" ")}
                
                [resource_limits]
                time = ${"%.2f".format(problemInfo.await().timeLimit / 1000.0)}s
                memory = ${problemInfo.await().memoryLimit}MiB
                
                [tests]
                group_pre = ${sampleTests.joinToString(" ") { it.index.toString() }}
                score_pre = 0
                continue_condition_pre = WHILE_OK
                score = 100
                continue_condition = ALWAYS
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
                    "Checker 'check.cpp' not found in the polygon problem package. " +
                            "Other kinds of checkers are not supported."
                )
            }
            Files.copy(checkerFilePath, checkerPath.resolve("check.cpp"))
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
            val mainSolution = polygonApi.getMainSolution(problemId)
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

        suspend fun writeTests(tests: List<PolygonTest>): List<Int> {
            try {
                fun writeTest(index: Int, type: String, content: String) {
                    Files.write(
                        testsPath.resolve("${index}.$type"),
                        content.toByteArray()
                    )
                }

                val inputs = tests.map {
                    async {
                        writeTest(it.index, "in", polygonApi.getTestInput(problemId, it.index))
                    }
                }
                val outputs = tests.map {
                    async {
                        writeTest(it.index, "out", polygonApi.getTestAnswer(problemId, it.index))
                    }
                }
                inputs.awaitAll()
                outputs.awaitAll()

                return tests.filter { it.useInStatements }.map { it.index }
            } catch (ex: Exception) {
                throw SybonArchiveBuildException("Failed to load test data: ${ex.message}", ex)
            }
        }

        val tests = polygonApi.getTests(problemId).result!!
        writeConfig(tests.filter { it.useInStatements })
        writeFormat()
        writeChecker()
        writeSolution()
        writeStatement()
        writeTests(tests)

        val actualPackageId = polygonApi.getLatestPackage(problemId)!!.id
        if (actualPackageId != packageId.await()) {
            getLogger(javaClass).info(
                "" +
                        "New package was created for problem ${problem.await()}. " +
                        "Old package id was ${packageId.await()}, actual is $actualPackageId. " +
                        "Rebuilding the sybon package."
            )
            return@coroutineScope buildInternal(problemId, properties)
        }

        val par = destinationPath.parent
        val zipPath = par.resolve("${problem.await().name}-rev${problem.await().revision}.zip")
        par.toZipArchive(zipPath)
        zipPath
    }
}
