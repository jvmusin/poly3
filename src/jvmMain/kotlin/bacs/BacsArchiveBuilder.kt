@file:OptIn(ExperimentalPathApi::class)

package bacs

import api.AdditionalProblemProperties
import api.AdditionalProblemProperties.Companion.defaultProperties
import ir.IRProblem
import util.toZipArchive
import java.nio.file.Path
import java.util.Locale
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes
import kotlin.io.path.writeText

/**
 * BACS archive builder.
 *
 * Used to pack [IRProblem] problems to zip archives.
 *
 * @property workFolder The folder to store temporary files in.
 */
class BacsArchiveBuilder(private val workFolder: Path) {

    /**
     * Packs [problem] into a zip-archive using extra [properties].
     *
     * @return File where the zip is located.
     */
    fun buildArchive(problem: IRProblem, properties: AdditionalProblemProperties = defaultProperties): Path {
        return problem.toZipArchive(properties)
    }

    /**
     * Packs `this` [IRProblem] into a zip-archive using extra [properties].
     *
     * @return File where the zip is located.
     */
    private fun IRProblem.toZipArchive(properties: AdditionalProblemProperties = defaultProperties): Path {
        val fullName = properties.buildFullName(name)
        val destinationPath = workFolder
            .resolve("sybon-packages")
            .resolve("$fullName-${UUID.randomUUID()}")
            .resolve(fullName)
        check(destinationPath.toFile().deleteRecursively()) { "Destination directory was not deleted" }

        val checkerPath = destinationPath.resolve("checker")
        val miscPath = destinationPath.resolve("misc")
        val solutionPath = miscPath.resolve("solution")
        val statementPath = destinationPath.resolve("statement")
        val testsPath = destinationPath.resolve("tests")
        for (path in arrayOf(destinationPath, checkerPath, miscPath, solutionPath, statementPath, testsPath))
            path.createDirectories()

        fun writeConfig() {
            val timeLimitMillis = properties.timeLimitMillis ?: limits.timeLimitMillis
            val memoryLimitMegabytes = properties.memoryLimitMegabytes ?: limits.memoryLimitMegabytes
            destinationPath.resolve("config.ini").writeText(
                """
                [info]
                name = ${statement.name}
                maintainers = ${setOf(owner, "Musin").joinToString(" ")}
                
                [resource_limits]
                time = ${"%.2f".format(Locale.ENGLISH, timeLimitMillis / 1000.0)}s
                memory = ${memoryLimitMegabytes}MiB
                
                [tests]
                group_pre = ${requireNotNull(tests).filter { it.isSample }.joinToString(" ") { it.index.toString() }}
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
            checkerPath.resolve("check.cpp").writeText(checker.content)
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
            solutionPath.resolve(mainSolution.name).writeText(mainSolution.content)
        }

        fun writeStatement() {
            statementPath.resolve("problem.pdf").writeBytes(statement.content.toByteArray())
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
                testsPath.resolve("$index.$type").writeText(content)
            }
            requireNotNull(tests)
            for (t in tests) writeTest(t.index, "in", t.input)
            for (t in tests) writeTest(t.index, "out", t.output)
        }

        writeConfig()
        writeFormat()
        writeChecker()
        writeMainSolution()
        writeStatement()
        writeTests()

        val parent = destinationPath.parent
        val zipPath = workFolder
            .resolve("ready")
            .resolve("${parent.fileName}.zip")
        parent.toZipArchive(zipPath)
        destinationPath.parent.toFile().deleteRecursively()
        return zipPath
    }
}
