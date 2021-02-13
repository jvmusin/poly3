@file:OptIn(ExperimentalPathApi::class)

package polygon

import util.extract
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readBytes

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun PolygonApi.downloadPackage(problemId: Int, packageId: Int): Path {
    val destination = Paths.get("polygon-problems").resolve("id$problemId-package$packageId")
    if (Files.notExists(destination)) {
        getPackage(problemId, packageId).use { archive ->
            val tempDir = Files.createTempDirectory("${destination.fileName}-")
            val archivePath = tempDir.resolve("archive.zip")
            Files.write(archivePath, archive.bytes())
            ZipFile(archivePath.toFile()).use { it.extract(destination) }
            Files.delete(archivePath)
        }
    }
    return destination
}

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun PolygonApi.getStatementRaw(
    problemId: Int,
    packageId: Int,
    type: String = "pdf",
    language: String = "russian"
): ByteArray? {
    val filePath = downloadPackage(problemId, packageId)
        .resolve("statements")
        .resolve(".$type")
        .resolve(language)
        .resolve("problem.$type")
    if (Files.notExists(filePath)) return null
    return filePath.readBytes()
}

suspend fun PolygonApi.getProblem(problemId: Int): Problem {
    return getProblems().result!!.single { it.id == problemId }
}

suspend fun PolygonApi.getStatement(problemId: Int, language: String? = null): Pair<String, Statement>? {
    val statements = getStatements(problemId).result!!
    if (statements.isEmpty()) return null
    return statements.entries.firstOrNull {
        language == null || it.key == language
    }?.let { it.key to it.value }
}

suspend fun PolygonApi.getLatestPackage(problemId: Int): Package? {
    return getPackages(problemId).result
        ?.filter { it.state == Package.State.READY }
        ?.maxByOrNull { it.id }
}