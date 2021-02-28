@file:OptIn(ExperimentalTime::class)

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import polygon.exception.ProblemDownloadingException
import polygon.PolygonService
import util.getLogger
import kotlin.time.ExperimentalTime

class PolygonProblemDownloadingTests : StringSpec({
    val service by inject(PolygonService::class.java)

    "Downloading problem without any built packages fails" {
        shouldThrow<ProblemDownloadingException> {
            service.downloadProblem(157557)
        }
    }

    "Downloading problem without any statement fails" {
        shouldThrow<ProblemDownloadingException> {
            service.downloadProblem(155265)
        }
    }

    "Downloading problem without pdf statement fails" {
        shouldThrow<ProblemDownloadingException> {
            service.downloadProblem(104916)
        }
    }

    "Downloading problem without WRITE access fails" {
        shouldThrow<ProblemDownloadingException> {
            service.downloadProblem(66010)
        }
    }

    "Downloading problem without 'check.cpp' checker fails" {
        shouldThrow<ProblemDownloadingException> {
            service.downloadProblem(82475)
        }
    }

    "Downloading modified problem fails" {
        shouldThrow<ProblemDownloadingException> {
            service.downloadProblem(157878)
        }
    }

    "Downloading problem without actual package fails" {
        shouldThrow<ProblemDownloadingException> {
            service.downloadProblem(157883)
        }
    }

    "!Download all the problems (without tests)" {
        val problems = service.getProblems().sortedBy { it.id }
        for ((i, p) in problems.withIndex()) {
            getLogger(javaClass).info("Downloading problem $${i + 1}/${problems.size} ${p.id}:${p.name}")
            try {
                service.downloadProblem(p.id, true)
            } catch (ignored: ProblemDownloadingException) {
                //that's alright
            }
        }
    }
}), KoinTest {
    override fun listeners() = listOf(KoinListener(polygonModule))
}