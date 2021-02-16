import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.collections.shouldBeSortedWith
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import polygon.PolygonApi
import polygon.PolygonTest
import polygon.polygonModule

class PolygonApiTests : StringSpec({
    val api by inject(PolygonApi::class.java)

    "getTests returns tests sorted by index" {
        val tests = api.getTests(144543).result!!
        tests shouldBeSortedWith compareBy(PolygonTest::index)
    }
}), KoinTest {
    override fun listeners() = listOf(KoinListener(polygonModule))
}