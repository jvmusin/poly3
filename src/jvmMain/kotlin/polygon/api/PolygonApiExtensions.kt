package polygon.api

import polygon.exception.response.NoSuchProblemException
import util.extract
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.notExists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

private val packagesCache = ConcurrentHashMap<Int, Path>()

@OptIn(ExperimentalPathApi::class)
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

@OptIn(ExperimentalPathApi::class)
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

/**
 * Returns problem with the given [problemId] from the problem list.
 *
 * @param problemId the problem id to return.
 * @return The problem.
 * @throws NoSuchProblemException if the problem is not found.
 * @see PolygonApi.getProblems
 */
suspend fun PolygonApi.getProblem(problemId: Int) = getProblems().extract().singleOrNull { it.id == problemId }
    ?: throw NoSuchProblemException("There is no problem with id $problemId")

suspend fun PolygonApi.getStatement(problemId: Int, language: String? = null): Pair<String, Statement>? {
    return getStatements(problemId).extract().entries.firstOrNull {
        language == null || it.key == language
    }?.let { it.key to it.value }
}

suspend fun PolygonApi.getLatestPackageId(problemId: Int): Int {
    return getPackages(problemId).extract()
        .filter { it.state == Package.State.READY }
        .maxOf { it.id }
}
