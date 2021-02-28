import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forAll
import io.kotest.koin.KoinListener
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.koin.test.KoinTest
import org.koin.test.inject
import polygon.PolygonApi
import polygon.TestGroup.PointsPolicyType.COMPLETE_GROUP
import polygon.TestGroup.PointsPolicyType.EACH_TEST

class PolygonApiTests : BehaviorSpec(), KoinTest {
    private val api: PolygonApi by inject()

    override fun listeners() = listOf(KoinListener(polygonModule))

    init {
        Given("getTestGroup") {
            When("asking for problem with test group on all tests") {

                val problemId = 159528
                suspend fun result() = api.getTestGroup(problemId, null).result!!
                val expectedGroups = listOf("samples", "first", "second", "third", "fourth", "fifth")

                Then("returns correct group names") {
                    result().map { it.name } should containExactlyInAnyOrder(expectedGroups)
                }

                Then("returns correct group dependencies") {
                    val byName = result().associateBy { it.name }
                    byName["second"]!!.dependencies should containExactlyInAnyOrder("samples")
                    byName["third"]!!.dependencies should containExactlyInAnyOrder("first", "fifth")
                    byName["fourth"]!!.dependencies should containExactlyInAnyOrder("third")
                    (expectedGroups - setOf("second", "third", "fourth")).forAll {
                        byName[it]!!.dependencies.shouldBeEmpty()
                    }
                }

                Then("all groups except fourth should have points policy of EACH_TEST") {
                    val result = result().associateBy { it.name }
                    result.entries.filter { it.key != "fourth" }.map { it.value }
                        .forAll { it.pointsPolicy shouldBe EACH_TEST }
                    result["fourth"]!!.pointsPolicy shouldBe COMPLETE_GROUP
                }

                Then("fourth group should have points policy of COMPLETE_GROUP") {
                    result().single { it.name == "fourth" }.pointsPolicy shouldBe COMPLETE_GROUP
                }
            }

            When("asking for problem with test group on all tests except samples") {

                val problemId = 159558
                suspend fun result() = api.getTestGroup(problemId, null).result!!
                val expectedGroups = listOf("first", "second", "third", "fourth", "fifth")

                Then("returns correct group names") {
                    result().map { it.name } should containExactlyInAnyOrder(expectedGroups)
                }

                Then("returns correct group dependencies") {
                    val byName = result().associateBy { it.name }
                    byName["third"]!!.dependencies should containExactlyInAnyOrder("first", "fifth")
                    byName["fourth"]!!.dependencies should containExactlyInAnyOrder("third")
                    (expectedGroups - setOf("third", "fourth")).forAll {
                        byName[it]!!.dependencies.shouldBeEmpty()
                    }
                }

                Then("all groups except fourth should have points policy of EACH_TEST") {
                    val result = result().associateBy { it.name }
                    result.entries.filter { it.key != "fourth" }.map { it.value }
                        .forAll { it.pointsPolicy shouldBe EACH_TEST }
                    result["fourth"]!!.pointsPolicy shouldBe COMPLETE_GROUP
                }

                Then("fourth group should have points policy of COMPLETE_GROUP") {
                    result().single { it.name == "fourth" }.pointsPolicy shouldBe COMPLETE_GROUP
                }
            }

            When("asking for problem with test groups and no points") {

                val problemId = 159559
                suspend fun result() = api.getTestGroup(problemId, null).result!!
                val expectedGroups = listOf("samples", "first", "second", "third", "fourth", "fifth")

                Then("returns correct group names") {
                    result().map { it.name } should containExactlyInAnyOrder(expectedGroups)
                }

                Then("returns group dependencies as if they were shown on the page") {
                    val byName = result().associateBy { it.name }
                    byName["second"]!!.dependencies should containExactlyInAnyOrder("samples")
                    byName["third"]!!.dependencies should containExactlyInAnyOrder("first", "fifth")
                    byName["fourth"]!!.dependencies should containExactlyInAnyOrder("third")
                    (expectedGroups - setOf("second", "third", "fourth")).forAll {
                        byName[it]!!.dependencies.shouldBeEmpty()
                    }
                }

                Then("all groups except fourth should have points policy of EACH_TEST") {
                    val result = result().associateBy { it.name }
                    result.entries.filter { it.key != "fourth" }.map { it.value }
                        .forAll { it.pointsPolicy shouldBe EACH_TEST }
                    result["fourth"]!!.pointsPolicy shouldBe COMPLETE_GROUP
                }

                Then("fourth group should have points policy of COMPLETE_GROUP") {
                    result().single { it.name == "fourth" }.pointsPolicy shouldBe COMPLETE_GROUP
                }
            }
        }
    }
}
