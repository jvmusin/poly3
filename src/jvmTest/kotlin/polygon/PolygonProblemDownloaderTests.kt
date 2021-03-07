package polygon

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ir.IRTest
import ir.IRTestGroup
import ir.IRTestGroupPointsPolicy
import org.koin.test.KoinTest
import org.koin.test.inject
import polygon.TestProblems.interactiveProblem
import polygon.TestProblems.modifiedProblem
import polygon.TestProblems.noBuiltPackagesProblem
import polygon.TestProblems.oldPackageProblem
import polygon.TestProblems.problemWhereSampleGoesSecond
import polygon.TestProblems.problemWhereSamplesAreFirstAndThirdTests
import polygon.TestProblems.problemWhereSamplesAreNotFormingFirstTestGroup
import polygon.TestProblems.problemWithMissingTestGroups
import polygon.TestProblems.problemWithNonSequentialTestIndices
import polygon.TestProblems.problemWithNonSequentialTestsInTestGroup
import polygon.TestProblems.problemWithNormalTestGroupsAndPoints
import polygon.TestProblems.problemWithOnlyReadAccess
import polygon.TestProblems.problemWithPointsOnSample
import polygon.TestProblems.problemWithPointsOnSamplesGroup
import polygon.TestProblems.problemWithoutCppChecker
import polygon.TestProblems.problemWithoutPdfStatement
import polygon.TestProblems.problemWithoutStatement
import polygon.TestProblems.totallyUnknownProblem
import polygon.exception.downloading.ProblemDownloadingException
import polygon.exception.downloading.format.ProblemModifiedException
import polygon.exception.downloading.format.UnsupportedFormatException
import polygon.exception.downloading.packages.NoPackagesBuiltException
import polygon.exception.downloading.packages.OldBuiltPackageException
import polygon.exception.downloading.resource.CheckerNotFoundException
import polygon.exception.downloading.resource.PdfStatementNotFoundException
import polygon.exception.downloading.resource.StatementNotFoundException
import polygon.exception.downloading.tests.MissingTestGroupException
import polygon.exception.downloading.tests.NonSequentialTestIndicesException
import polygon.exception.downloading.tests.NonSequentialTestsInTestGroupException
import polygon.exception.downloading.tests.PointsOnSampleException
import polygon.exception.downloading.tests.SamplesNotFirstException
import polygon.exception.downloading.tests.SamplesNotFormingFirstTestGroupException
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
                Then("throws ProblemModifiedException") {
                    downloadProblemWithException<ProblemModifiedException>(modifiedProblem)
                }
            }
            When("no packages build") {
                Then("throws NoPackagesBuiltException") {
                    downloadProblemWithException<NoPackagesBuiltException>(noBuiltPackagesProblem)
                }
            }
            When("last built package is old") {
                Then("throws OldBuiltPackageException") {
                    downloadProblemWithException<OldBuiltPackageException>(oldPackageProblem)
                }
            }
            When("problem is interactive") {
                Then("throws UnsupportedFormatException") {
                    downloadProblemWithException<UnsupportedFormatException>(interactiveProblem)
                }
            }
            When("problem has no statement") {
                Then("throws StatementNotFoundException") {
                    downloadProblemWithException<StatementNotFoundException>(problemWithoutStatement)
                }
            }
            When("problem has no pdf statement") {
                Then("throws PdfStatementNotFoundException") {
                    downloadProblemWithException<PdfStatementNotFoundException>(problemWithoutPdfStatement)
                }
            }
            When("problem has no cpp checker") {
                Then("throws CheckerNotFoundException") {
                    downloadProblemWithException<CheckerNotFoundException>(problemWithoutCppChecker)
                }
            }
            // Disabled because Polygon does not allow such kind of tests
            xWhen("problem has missing test indices") {
                Then("throws NonSequentialTestIndicesException") {
                    downloadProblemWithException<NonSequentialTestIndicesException>(problemWithNonSequentialTestIndices)
                }
            }
            When("sample is only second test") {
                Then("throws SamplesNotFirstException") {
                    downloadProblemWithException<SamplesNotFirstException>(problemWhereSampleGoesSecond)
                }
            }
            When("samples are first and third tests") {
                Then("throws SamplesNotFirstException") {
                    downloadProblemWithException<SamplesNotFirstException>(problemWhereSamplesAreFirstAndThirdTests)
                }
            }
            When("test groups are enabled") {
                And("some tests don't have a group") {
                    Then("throws MissingTestGroupException") {
                        downloadProblemWithException<MissingTestGroupException>(problemWithMissingTestGroups)
                    }
                }
                And("tests within the same group don't go one after another") {
                    Then("throws NonSequentialTestsInTestGroupException") {
                        downloadProblemWithException<NonSequentialTestsInTestGroupException>(
                            problemWithNonSequentialTestsInTestGroup
                        )
                    }
                }
                And("samples don't form first test group") {
                    Then("throws SamplesNotFormingFirstTestGroupException") {
                        downloadProblemWithException<SamplesNotFormingFirstTestGroupException>(
                            problemWhereSamplesAreNotFormingFirstTestGroup
                        )
                    }
                }
                And("sample has points") {
                    Then("throws PointsOnSampleException") {
                        downloadProblemWithException<PointsOnSampleException>(problemWithPointsOnSample)
                    }
                }
                And("samples group has points") {
                    Then("throws PointsOnSampleException") {
                        downloadProblemWithException<PointsOnSampleException>(problemWithPointsOnSamplesGroup)
                    }
                }
                And("everything is alright") {
                    Then("downloads tests correctly") {
                        with(downloader.downloadProblem(problemWithNormalTestGroupsAndPoints, true)) {
                            tests shouldBe listOf(
                                IRTest(1, true, "1\r\n", "ans1\r\n", null, "samples"),
                                IRTest(2, false, "2\r\n", "ans2\r\n", null, "first"),
                                IRTest(3, false, "3\r\n", "ans3\r\n", null, "first"),
                                IRTest(4, false, "4\r\n", "ans4\r\n", 5, "second"),
                                IRTest(5, false, "5\r\n", "ans5\r\n", 5, "second"),
                                IRTest(6, false, "6\r\n", "ans6\r\n", 0, "third")
                            )
                        }
                    }
                    Then("downloads test groups correctly") {
                        with(downloader.downloadProblem(problemWithNormalTestGroupsAndPoints, false)) {
                            groups shouldBe listOf(
                                IRTestGroup("samples", IRTestGroupPointsPolicy.NO_POINTS, listOf(1), null),
                                IRTestGroup("first", IRTestGroupPointsPolicy.COMPLETE_GROUP, listOf(2, 3), 10),
                                IRTestGroup("second", IRTestGroupPointsPolicy.EACH_TEST, listOf(4, 5), null),
                                IRTestGroup("third", IRTestGroupPointsPolicy.EACH_TEST, listOf(6), null),
                            )
                        }
                    }
                }
            }
        }
    }
}
