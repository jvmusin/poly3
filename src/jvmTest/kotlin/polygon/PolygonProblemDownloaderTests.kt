package polygon

import polygon.TestProblems.interactiveProblem
import polygon.TestProblems.modifiedProblem
import polygon.TestProblems.noBuiltPackagesProblem
import polygon.TestProblems.oldPackageProblem
import polygon.TestProblems.problemWithOnlyReadAccess
import polygon.TestProblems.problemWithoutCppChecker
import polygon.TestProblems.problemWithoutPdfStatement
import polygon.TestProblems.problemWithoutStatement
import polygon.TestProblems.totallyUnknownProblem
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.types.shouldBeInstanceOf
import org.koin.test.KoinTest
import org.koin.test.inject
import polygon.exception.downloading.ProblemDownloadingException
import polygon.exception.downloading.format.ProblemModifiedException
import polygon.exception.downloading.format.UnsupportedFormatException
import polygon.exception.downloading.packages.NoPackagesBuiltException
import polygon.exception.downloading.packages.OldBuiltPackageException
import polygon.exception.downloading.resource.CheckerNotFoundException
import polygon.exception.downloading.resource.PdfStatementNotFoundException
import polygon.exception.downloading.resource.StatementNotFoundException
import polygon.exception.response.AccessDeniedException
import polygon.exception.response.NoSuchProblemException
import polygonModule

class PolygonProblemDownloaderTests : BehaviorSpec(), KoinTest {
    override fun listeners() = listOf(KoinListener(polygonModule))

    private val downloader: PolygonProblemDownloader by inject()

    private suspend inline fun <reified TException : Throwable> downloadProblemWithException(
        problemId: Int,
        includeTests: Boolean = false
    ) {
        shouldThrowExactly<TException> { downloader.downloadProblem(problemId, includeTests) }
            .shouldBeInstanceOf<ProblemDownloadingException>()
    }

    init {
        Given("downloadProblem") {
            When("problem is unknown") {
                Then("throws NoSuchProblemException") {
                    shouldThrowExactly<NoSuchProblemException> {
                        downloader.downloadProblem(totallyUnknownProblem, false)
                    }
                }
            }
            When("no WRITE access") {
                Then("throws AccessDeniedException") {
                    shouldThrowExactly<AccessDeniedException> {
                        downloader.downloadProblem(problemWithOnlyReadAccess, false)
                    }
                }
            }
            When("problem is modified") {
                Then("throws ProblemDownloadingException with cause ProblemModifiedException") {
                    downloadProblemWithException<ProblemModifiedException>(modifiedProblem)
                }
            }
            When("no packages build") {
                Then("throws ProblemDownloadingException with cause NoPackagesBuiltException") {
                    downloadProblemWithException<NoPackagesBuiltException>(noBuiltPackagesProblem)
                }
            }
            When("last built package is old") {
                Then("throws ProblemDownloadingException with cause OldBuiltPackageException") {
                    downloadProblemWithException<OldBuiltPackageException>(oldPackageProblem)
                }
            }
            When("problem is interactive") {
                Then("throws ProblemDownloadingException with cause UnsupportedFormatException") {
                    downloadProblemWithException<UnsupportedFormatException>(interactiveProblem)
                }
            }
            When("problem has no statement") {
                Then("throws ProblemDownloadingException with cause StatementNotFoundException") {
                    downloadProblemWithException<StatementNotFoundException>(problemWithoutStatement)
                }
            }
            When("problem has no pdf statement") {
                Then("throws ProblemDownloadingException with cause PdfStatementNotFoundException") {
                    downloadProblemWithException<PdfStatementNotFoundException>(problemWithoutPdfStatement)
                }
            }
            When("problem has no cpp checker") {
                Then("throws ProblemDownloadingException with cause CheckerNotFoundException") {
                    downloadProblemWithException<CheckerNotFoundException>(problemWithoutCppChecker)
                }
            }
        }
    }
}
