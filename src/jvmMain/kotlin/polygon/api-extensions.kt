package polygon

import extract
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

@Suppress("BlockingMethodInNonBlockingContext")
suspend fun ProblemApi.downloadPackage(problemId: Int, packageId: Int): Path {
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
suspend fun ProblemApi.getStatementRaw(
    problemId: Int,
    packageId: Int,
    type: String = "pdf",
    language: String = "russian"
): ByteArray {
    val filePath = downloadPackage(problemId, packageId)
        .resolve("statements")
        .resolve(".$type")
        .resolve(language)
        .resolve("problem.$type")
    return Files.readAllBytes(filePath)
}

suspend fun ProblemApi.getProblem(problemId: Int): Problem {
    return getProblems().result!!.first { it.id == problemId }
}

suspend fun ProblemApi.getStatement(problemId: Int, language: String? = null): Pair<String, Statement> {
    return getStatements(problemId).result!!.entries.firstOrNull {
        language == null || it.key == language
    }!!.let { it.key to it.value }
}