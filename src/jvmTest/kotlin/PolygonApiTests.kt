import TestProblems.problemWithOnlyReadAccess
import TestProblems.problemWithTestGroups
import TestProblems.problemWithTestGroupsExceptSamples
import TestProblems.problemWithoutPdfStatement
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forAll
import io.kotest.koin.KoinListener
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.koin.test.KoinTest
import org.koin.test.inject
import polygon.api.PolygonApi
import polygon.api.TestGroup.PointsPolicyType.COMPLETE_GROUP
import polygon.api.TestGroup.PointsPolicyType.EACH_TEST

class PolygonApiTests : BehaviorSpec(), KoinTest {
    private val api: PolygonApi by inject()

    override fun listeners() = listOf(KoinListener(polygonModule))

    init {
        Given("getTestGroup") {
            When("asking for problem with test group on all tests") {

                suspend fun result() = api.getTestGroup(problemWithTestGroups).extract()
                val expectedGroups = listOf("samples", "first", "second", "third", "fourth", "fifth")

                Then("returns correct group names") {
                    result().map { it.name } should containExactlyInAnyOrder(expectedGroups)
                }

                Then("returns correct group dependencies") {
                    val byName = result().associateBy { it.name }
                    byName["second"].shouldNotBeNull().dependencies should containExactlyInAnyOrder("samples")
                    byName["third"].shouldNotBeNull().dependencies should containExactlyInAnyOrder("first", "fifth")
                    byName["fourth"].shouldNotBeNull().dependencies should containExactlyInAnyOrder("third")
                    (expectedGroups - setOf("second", "third", "fourth")).forAll {
                        byName[it].shouldNotBeNull().dependencies.shouldBeEmpty()
                    }
                }

                Then("all groups except fourth should have points policy of EACH_TEST") {
                    val result = result().associateBy { it.name }
                    result.entries.filter { it.key != "fourth" }.map { it.value }
                        .forAll { it.pointsPolicy shouldBe EACH_TEST }
                    result["fourth"].shouldNotBeNull().pointsPolicy shouldBe COMPLETE_GROUP
                }

                Then("fourth group should have points policy of COMPLETE_GROUP") {
                    result().single { it.name == "fourth" }.pointsPolicy shouldBe COMPLETE_GROUP
                }
            }

            When("asking for problem with test group on all tests except samples") {

                suspend fun result() = api.getTestGroup(problemWithTestGroupsExceptSamples).extract()
                val expectedGroups = listOf("first", "second", "third", "fourth", "fifth")

                Then("returns correct group names") {
                    result().map { it.name } should containExactlyInAnyOrder(expectedGroups)
                }

                Then("returns correct group dependencies") {
                    val byName = result().associateBy { it.name }
                    byName["third"].shouldNotBeNull().dependencies should containExactlyInAnyOrder("first", "fifth")
                    byName["fourth"].shouldNotBeNull().dependencies should containExactlyInAnyOrder("third")
                    (expectedGroups - setOf("third", "fourth")).forAll {
                        byName[it].shouldNotBeNull().dependencies.shouldBeEmpty()
                    }
                }

                Then("all groups except fourth should have points policy of EACH_TEST") {
                    val result = result().associateBy { it.name }
                    result.entries.filter { it.key != "fourth" }.map { it.value }
                        .forAll { it.pointsPolicy shouldBe EACH_TEST }
                    result["fourth"].shouldNotBeNull().pointsPolicy shouldBe COMPLETE_GROUP
                }

                Then("fourth group should have points policy of COMPLETE_GROUP") {
                    result().single { it.name == "fourth" }.pointsPolicy shouldBe COMPLETE_GROUP
                }
            }

            When("asking for problem with no test groups") {
                Then("returns null result and appropriate status and comment fields") {
                    with(api.getTestGroup(problemWithoutPdfStatement)) {
                        status shouldBe "FAILED"
                        result.shouldBeNull()
                        comment shouldBe "testset: Test groups are disabled for the specified testset"
                    }
                }
            }
        }

        Given("getProblemInfo") {
            When("asking for problem with OWNER access") {
                Then("returns info") {
                    with(api.getProblemInfo(problemWithTestGroups).result.shouldNotBeNull()) {
                        inputFile shouldBe "stdin"
                        outputFile shouldBe "stdout"
                        interactive shouldBe false
                        timeLimit shouldBe 1000
                        memoryLimit shouldBe 256
                    }
                }
            }
            When("asking for problem with READ access") {
                Then("returns problem not found") {
                    with(api.getProblemInfo(problemWithOnlyReadAccess)) {
                        status shouldBe "FAILED"
                        result.shouldBeNull()
                        comment shouldBe "problemId: Problem not found"
                    }
                }
            }
        }
    }
}
