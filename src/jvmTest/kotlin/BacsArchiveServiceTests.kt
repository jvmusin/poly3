import bacs.BacsArchiveService
import bacs.BacsProblemState.IMPORTED
import bacs.BacsProblemState.NOT_FOUND
import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.shouldBe
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest

class BacsArchiveServiceTests :
    StringSpec({
        val service by inject(BacsArchiveService::class.java)

        "Get status of correctly imported problem" {
            service.getProblemState("polybacs-frog-and-polygon-ok") shouldBe IMPORTED
        }

        "Get status of not existing problem" {
            service.getProblemState("not-existing-problem") shouldBe NOT_FOUND
        }
    }),
    KoinTest {
    override fun listeners() = listOf(KoinListener(bacsModule))
}
