package util

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.streams.toList
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

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