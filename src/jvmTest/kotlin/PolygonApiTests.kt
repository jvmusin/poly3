import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.collections.shouldHaveSize
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import polygon.PolygonApi
import polygon.polygonModule
import util.retrofitModule

class PolygonApiTests : StringSpec({
    val api by inject(PolygonApi::class.java)

    "getTestGroup works" {
        val result = api.getTestGroup(144845, null).result!!
        println(result.size)
        result shouldHaveSize 4
    }

}), KoinTest {
    override fun listeners() = listOf(KoinListener(retrofitModule + polygonModule))
}