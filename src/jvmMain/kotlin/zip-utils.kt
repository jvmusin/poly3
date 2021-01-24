import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.streams.toList

fun ZipFile.extract(destination: Path) {
    Files.createDirectories(destination)
    for (entry in entries()) {
        if (entry.isDirectory) {
            Files.createDirectories(destination.resolve(entry.name))
        } else {
            getInputStream(entry).use {
                Files.write(destination.resolve(entry.name), it.readAllBytes())
            }
        }
    }
}

fun Path.toZipArchive(destination: Path) {
    val files = Files.walk(this)
        .map { it.toAbsolutePath() }
        .filter { it != toAbsolutePath() && it != destination.toAbsolutePath() && Files.isRegularFile(it) }
        .toList()
    Files.createDirectories(destination.parent)
    Files.deleteIfExists(destination)
    Files.createFile(destination)

    fun writeToZipFile(path: Path, zipStream: ZipOutputStream) {
        getLogger(javaClass).debug("Writing file : '$path' to zip file")
        val fis = Files.newInputStream(path)
        val zipEntry = ZipEntry(toAbsolutePath().relativize(path).toString().replace('\\', '/'))
        zipStream.putNextEntry(zipEntry)
        val bytes = ByteArray(1024)
        var length: Int
        while (fis.read(bytes).also { length = it } >= 0) {
            zipStream.write(bytes, 0, length)
        }
        zipStream.closeEntry()
        fis.close()
    }

    FileOutputStream(destination.toFile()).use { fos ->
        BufferedOutputStream(fos).use { bufOS ->
            ZipOutputStream(bufOS).use { zipOS ->
                for (file in files) {
                    writeToZipFile(file, zipOS)
                }
            }
        }
    }
}