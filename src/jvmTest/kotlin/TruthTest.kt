import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.shouldBe
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest

class TruthTest : StringSpec({
    val theTruth by inject(String::class.java)

    "Says the truth" {
        theTruth shouldBe "Kotlin is better than Java"
    }

}), KoinTest {
    override fun listeners() = listOf(KoinListener(module { single { "Kotlin is better than Java" } }))
}
