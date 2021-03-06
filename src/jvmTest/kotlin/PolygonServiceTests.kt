@file:OptIn(ExperimentalTime::class)

import TestProblems.noBuiltPackagesProblem
import TestProblems.problemWithOnlyReadAccess
import TestProblems.totallyUnknownProblem
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveCauseOfType
import org.koin.test.KoinTest
import org.koin.test.inject
import polygon.PolygonService
import polygon.exception.downloading.ProblemDownloadingException
import polygon.exception.downloading.resource.CheckerNotFoundException
import polygon.exception.response.NoSuchProblemException
import kotlin.time.ExperimentalTime

class PolygonServiceTests : BehaviorSpec(), KoinTest {
    private val service: PolygonService by inject()

    override fun listeners() = listOf(KoinListener(polygonModule))

    private suspend inline fun <reified TException : Throwable> downloadProblemWithInnerException(
        problemId: Int,
        includeTests: Boolean = false
    ) {
        shouldThrowExactly<ProblemDownloadingException> {
            service.downloadProblem(problemId, includeTests)
        }.shouldHaveCauseOfType<TException>()
    }

    init {
        Given("getProblemInfo") {
            When("have WRITE access") {
                Then("returns info") {
                    with(service.getProblemInfo(noBuiltPackagesProblem)) {
                        inputFile shouldBe "stdin"
                        outputFile shouldBe "stdout"
                        interactive shouldBe false
                        timeLimit shouldBe 1000
                        memoryLimit shouldBe 256
                    }
                }
            }
            When("have READ access") {
                Then("throws NoSuchProblemException") {
                    shouldThrowExactly<NoSuchProblemException> { service.getProblemInfo(problemWithOnlyReadAccess) }
                }
            }
            When("accessing unknown problem") {
                Then("throws NoSuchProblemException") {
                    shouldThrowExactly<NoSuchProblemException> { service.getProblemInfo(totallyUnknownProblem) }
                }
            }
        }

        xGiven("getProblems") {
            When("there is a problem with no appropriate checker") {
                Then("it's found and logged and then an exception is thrown") {
                    for (problem in service.getProblems()) {
                        try {
                            service.downloadProblem(problem.id)
                        } catch (e: ProblemDownloadingException) {
                            if (e.cause!!::class == CheckerNotFoundException::class) {
                                System.err.println("FOUND A PROBLEM WITH NO CPP CHECKER: ${problem.id}")
                                throw e
                            } else {
                                System.err.println("PROBLEM ${problem.id} FAILED WITH AN EXCEPTION ${e.message}")
                            }
                        }
                    }
                }
            }
        }
    }
}
