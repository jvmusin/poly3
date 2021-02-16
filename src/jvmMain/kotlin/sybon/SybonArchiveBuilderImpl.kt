@file:OptIn(ExperimentalPathApi::class)

package sybon

import api.AdditionalProblemProperties
import ir.IRProblem
import kotlinx.coroutines.coroutineScope
import util.toZipArchive
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

class SybonArchiveBuilderImpl : SybonArchiveBuilder {

    companion object {
        private const val BUILT_PACKAGES_FOLDER = "ready"
    }

    override suspend fun build(problem: IRProblem, properties: AdditionalProblemProperties): Path {
        return coroutineScope {
            val fullName = properties.buildFullName(problem.name)
            val destinationPath = Paths.get(
                "sybon-packages",
                "$fullName-${UUID.randomUUID()}",
                fullName
            )
            check(destinationPath.toFile().deleteRecursively()) { "Destination directory was not deleted" }

            val checkerPath = destinationPath.resolve("checker")
            val miscPath = destinationPath.resolve("misc")
            val solutionPath = miscPath.resolve("solution")
            val statementPath = destinationPath.resolve("statement")
            val testsPath = destinationPath.resolve("tests")

            for (path in arrayOf(destinationPath, checkerPath, miscPath, solutionPath, statementPath, testsPath))
                path.createDirectories()

            fun writeConfig() {
                val timeLimitMillis = properties.timeLimitMillis ?: problem.limits.timeLimitMillis
                val memoryLimitMegabytes = properties.memoryLimitMegabytes ?: problem.limits.memoryLimitMegabytes
                destinationPath.resolve("config.ini").writeText(
                    """
                        [info]
                        name = ${problem.statement.name}
                        maintainers = ${setOf(problem.owner, "Musin").joinToString(" ")}
                        
                        [resource_limits]
                        time = ${"%.2f".format(Locale.ENGLISH, timeLimitMillis / 1000.0)}s
                        memory = ${memoryLimitMegabytes}MiB
                        
                        [tests]
                        group_pre = ${problem.tests.filter { it.isSample }.joinToString(" ") { it.index.toString() }}
                        score_pre = 0
                        continue_condition_pre = WHILE_OK
                        score = 100
                        continue_condition = ALWAYS
                    """.trimIndent()
                )
            }

            fun writeFormat() {
                destinationPath.resolve("format").writeText("bacs/problem/single#simple0")
            }

            fun writeChecker() {
                checkerPath.resolve("check.cpp").writeText(problem.checker.content)
                checkerPath.resolve("config.ini").writeText(
                    """
                        [build]
                        builder = single
                        source = check.cpp
                        libs = testlib.googlecode.com-0.9.12

                        [utility]
                        call = in_out_hint
                        return = testlib
                    """.trimIndent()
                )
            }

            fun writeMainSolution() {
                val mainSolution = problem.mainSolution
                solutionPath.resolve(mainSolution.name).writeText(mainSolution.content)
            }

            fun writeStatement() {
                statementPath.resolve("problem.pdf").writeBytes(problem.statement.content)
                statementPath.resolve("pdf.ini").writeText(
                    """
                        [info]
                        language = C
        
                        [build]
                        builder = copy
                        source = problem.pdf
                    """.trimIndent()
                )
            }

            fun writeTests() {
                fun writeTest(index: Int, type: String, content: String) {
                    testsPath.resolve("${index}.$type").writeText(content)
                }
                for (t in problem.tests) writeTest(t.index, "in", t.input)
                for (t in problem.tests) writeTest(t.index, "out", t.output)
            }

            writeConfig()
            writeFormat()
            writeChecker()
            writeMainSolution()
            writeStatement()
            writeTests()

            val par = destinationPath.parent
            val zipPath = Paths.get(BUILT_PACKAGES_FOLDER, "${par.fileName}.zip")
            par.toZipArchive(zipPath)
            zipPath
        }
    }
}