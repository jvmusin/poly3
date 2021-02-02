import io.kotest.core.spec.style.StringSpec
import sybon.SybonApiFactory

class SybonTempTests : StringSpec({
    val api = SybonApiFactory().createArchiveApi()

    "test" {
        val problems = api.getCollection(1).problems.filter { it.toString().contains("polybacs") }
        println(problems.size)
        problems.forEach(::println)
    }
})