import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeSortedWith
import polygon.PolygonApiFactory
import polygon.PolygonTest
import sybon.SybonArchiveBuilder

@Ignored
class PolygonApiTests : StringSpec({
    val api = PolygonApiFactory().create()

    "getTests returns tests sorted by index" {
        val tests = api.getTests(144543).result!!
        tests shouldBeSortedWith compareBy(PolygonTest::index)
    }

    "test build archive" {
        val builder = SybonArchiveBuilder(api)
//        builder.build(LARGE_ARCHIVE_PROBLEM_ID, SybonArchiveProperties())
    }
}) {
    companion object {
        const val LARGE_ARCHIVE_PROBLEM_ID = 144845
    }
}