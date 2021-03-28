package util

import org.slf4j.LoggerFactory.getLogger
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.streams.toList
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

/** Extracts content of `this` [ZipFile] to [destination] folder. */
@OptIn(ExperimentalPathApi::class)
fun ZipFile.extract(destination: Path) {
    Files.createDirectories(destination)
    for (entry in entries()) {
        if (entry.isDirectory) {
            destination.resolve(entry.name).createDirectories()
        } else {
            getInputStream(entry).use {
                destination.resolve(entry.name).toFile().writeBytes(it.readAllBytes())
            }
        }
    }
}

/** Packs content of `this` [Path] to [destination] file. Rewrites [destination] file if it exists. */
@OptIn(ExperimentalTime::class)
fun Path.toZipArchive(destination: Path) {
    val (zipPath, duration) = measureTimedValue {
        val sourceAbsolutePath = toAbsolutePath()
        val destinationAbsolutePath = destination.toAbsolutePath()
        val files = Files.walk(this)
            .map { it.toAbsolutePath() }
            .filter { it != sourceAbsolutePath && it != destinationAbsolutePath && Files.isRegularFile(it) }
            .toList()
        Files.createDirectories(destination.parent)
        Files.deleteIfExists(destination)
        Files.createFile(destination)

        fun writeToZipFile(path: Path, zipStream: ZipOutputStream) {
            getLogger(javaClass).debug("Writing file : '$path' to zip file")
            val zipEntry = ZipEntry(sourceAbsolutePath.relativize(path).toString().replace('\\', '/'))
            zipStream.putNextEntry(zipEntry)
            Files.copy(path, zipStream)
            zipStream.closeEntry()
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
    getLogger(javaClass).info("Zip archive $destination built in $duration")
    return zipPath
}
