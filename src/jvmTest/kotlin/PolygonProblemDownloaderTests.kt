@file:OptIn(ExperimentalTime::class)

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import polygon.PolygonApi
import polygon.PolygonProblemDownloader
import polygon.PolygonProblemDownloaderException
import polygon.polygonModule
import util.getLogger
import util.retrofitModule
import kotlin.time.ExperimentalTime

class PolygonProblemDownloaderTests : StringSpec({
    val api by inject(PolygonApi::class.java)
    val downloader by inject(PolygonProblemDownloader::class.java)

    "Downloading problem without any built packages fails" {
        shouldThrow<PolygonProblemDownloaderException> {
            downloader.download(157608)
        }
    }

    "Downloading problem without any statement fails" {
        shouldThrow<PolygonProblemDownloaderException> {
            downloader.download(155265)
        }
    }

    "Downloading problem without pdf statement fails" {
        shouldThrow<PolygonProblemDownloaderException> {
            downloader.download(104916)
        }
    }

    "Downloading problem without WRITE access fails" {
        shouldThrow<PolygonProblemDownloaderException> {
            downloader.download(66010)
        }
    }

    "Downloading problem without 'check.cpp' checker fails" {
        shouldThrow<PolygonProblemDownloaderException> {
            downloader.download(82475)
        }
    }

    "Downloading modified problem fails" {
        shouldThrow<PolygonProblemDownloaderException> {
            downloader.download(157878)
        }
    }

    "Downloading problem without actual package fails" {
        shouldThrow<PolygonProblemDownloaderException> {
            downloader.download(157883)
        }
    }

    "!Download all the problems (without tests)" {
        val problems = api.getProblems().result!!.sortedBy { it.id }
        for ((i, p) in problems.withIndex()) {
            getLogger(javaClass).info("Downloading problem $${i + 1}/${problems.size} ${p.id}:${p.name}")
            try {
                downloader.download(p.id, true)
            } catch (ignored: PolygonProblemDownloaderException) {
                //that's alright
            }
        }
    }
}), KoinTest {
    override fun listeners() = listOf(KoinListener(polygonModule + retrofitModule))
}