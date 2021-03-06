import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.koin.KoinListener
import org.koin.test.KoinTest
import org.koin.test.inject
import polygon.api.PolygonApi
import polygon.exception.response.NoSuchProblemException
import polygon.exception.response.NoSuchTestGroupException
import polygon.exception.response.TestGroupsDisabledException

class PolygonResponseTests : BehaviorSpec(), KoinTest {
    override fun listeners() = listOf(KoinListener(polygonModule))

    private val api: PolygonApi by inject()

    init {
        Given("extract") {
            When("requested unknown problem") {
                Then("throws NoSuchProblemException") {
                    shouldThrowExactly<NoSuchProblemException> {
                        api.getProblemInfo(TestProblems.totallyUnknownProblem).extract()
                    }
                }
            }
            When("requested test groups from problem with no test groups") {
                Then("throws TestGroupsDisabledException") {
                    shouldThrowExactly<TestGroupsDisabledException> {
                        api.getTestGroup(TestProblems.problemWithoutPdfStatement).extract()
                    }
                }
            }
            When("requested unknown test group") {
                Then("throws NoSuchTestGroupException") {
                    shouldThrowExactly<NoSuchTestGroupException> {
                        api.getTestGroup(TestProblems.problemWithTestGroups, "unknown-test-group").extract()
                    }
                }
            }
        }
    }
}
