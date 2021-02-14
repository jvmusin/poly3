@file:OptIn(ExperimentalPathApi::class)

package polygon

import util.extract
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.notExists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

private val packagesCache = ConcurrentHashMap<Int, Path>()

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun PolygonApi.downloadPackage(problemId: Int, packageId: Int): Path {
    if (packagesCache.containsKey(packageId)) return packagesCache[packageId]!!
    val destination = Paths.get("polygon-problems").resolve("id$problemId-package$packageId-${UUID.randomUUID()}")
    val archivePath = Files.createTempDirectory("${destination.fileName}-").resolve("archive.zip")
    archivePath.writeBytes(getPackage(problemId, packageId).bytes())
    ZipFile(archivePath.toFile()).use { it.extract(destination) }
    Files.delete(archivePath)
    return destination.also { packagesCache[packageId] = it }
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
    if (filePath.notExists()) return null
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