import io.github.config4k.extract
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
import polygon.polygonModule

class PolygonApiTests : BehaviorSpec(), KoinTest {
    private val problemId = 159528
    private val api: PolygonApi by inject()

    override fun listeners() = listOf(KoinListener(polygonModule(config.extract("polygon"))))

    init {
        Given("getTestGroup") {
            When("asking for problem with test group on all tests") {

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
        }
    }
}
