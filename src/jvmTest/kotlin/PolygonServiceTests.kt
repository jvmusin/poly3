@file:OptIn(ExperimentalTime::class)

import TestProblems.problemWithNoWriteAccess
import TestProblems.interactiveProblem
import TestProblems.modifiedProblem
import TestProblems.noBuildPackagesProblem
import TestProblems.oldPackageProblem
import TestProblems.problemWithoutCppChecker
import TestProblems.problemWithoutPdfStatement
import TestProblems.problemWithoutStatement
import TestProblems.totallyUnknownProblem
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.throwable.shouldHaveCauseOfType
import org.koin.test.KoinTest
import org.koin.test.inject
import polygon.PolygonService
import polygon.exception.*
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
        Given("downloadProblem") {
            When("problem is unknown") {
                Then("throws ProblemDownloadingException with cause NoSuchProblemException") {
                    downloadProblemWithInnerException<NoSuchProblemException>(totallyUnknownProblem)
                }
            }
            When("no WRITE access") {
                Then("throws ProblemDownloadingException with cause AccessDeniedException") {
                    downloadProblemWithInnerException<AccessDeniedException>(problemWithNoWriteAccess)
                }
            }
            When("problem is modified") {
                Then("throws ProblemDownloadingException with cause NoPackagesBuiltException") {
                    downloadProblemWithInnerException<ProblemModifiedException>(modifiedProblem)
                }
            }
            When("no packages build") {
                Then("throws ProblemDownloadingException with cause NoPackagesBuiltException") {
                    downloadProblemWithInnerException<NoPackagesBuiltException>(noBuildPackagesProblem)
                }
            }
            When("last built package is old") {
                Then("throws ProblemDownloadingException with cause OldBuiltPackageException") {
                    downloadProblemWithInnerException<OldBuiltPackageException>(oldPackageProblem)
                }
            }
            When("problem is interactive") {
                Then("throws ProblemDownloadingException with cause UnsupportedFormatException") {
                    downloadProblemWithInnerException<UnsupportedFormatException>(interactiveProblem)
                }
            }
            When("problem has no statement") {
                Then("throws ProblemDownloadingException with cause StatementNotFoundException") {
                    downloadProblemWithInnerException<StatementNotFoundException>(problemWithoutStatement)
                }
            }
            When("problem has no pdf statement") {
                Then("throws ProblemDownloadingException with cause PdfStatementNotFoundException") {
                    downloadProblemWithInnerException<PdfStatementNotFoundException>(problemWithoutPdfStatement)
                }
            }
            When("problem has no cpp checker") {
                Then("throws ProblemDownloadingException with cause CheckerNotFoundException") {
                    downloadProblemWithInnerException<CheckerNotFoundException>(problemWithoutCppChecker)
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
                                System.err.println("FOUND A PROBLEM WITH NO CPP CHECKER STATEMENT: ${problem.id}")
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
