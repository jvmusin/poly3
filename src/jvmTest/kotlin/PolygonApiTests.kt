import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeSortedWith
import polygon.PolygonApiFactory
import polygon.PolygonTest

@Ignored
class PolygonApiTests : StringSpec({
    val api = PolygonApiFactory().create()

    "getTests returns tests sorted by index" {
        val tests = api.getTests(144543).result!!
        tests shouldBeSortedWith compareBy(PolygonTest::index)
    }
})