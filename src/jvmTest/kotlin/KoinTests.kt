import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest

class KoinTests : StringSpec({
    val x by inject(String::class.java)

    "Print x" {
        println(x)
    }

}), KoinTest {
    override fun listeners() = listOf(KoinListener(module { single { "aww" } }))
}