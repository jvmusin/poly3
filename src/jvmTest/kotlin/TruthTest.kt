import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.shouldBe
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject

class TruthTest : StringSpec(), KoinTest {
    override fun listeners() = listOf(KoinListener(module { single { "used instead of Java" } }))

    private val kotlin: String by inject()

    init {
        "Says the truth" {
            kotlin shouldBe "used instead of Java"
        }
    }
}
