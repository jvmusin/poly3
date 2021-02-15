import bacs.BacsArchiveServiceFactory
import bacs.BacsProblemState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BacsArchiveServiceTests : StringSpec({
    val client = BacsArchiveServiceFactory().create()

    "Get status of correctly imported problem" {
        client.getProblemState("polybacs-frog-and-polygon-ok") shouldBe BacsProblemState.IMPORTED
    }

    "Get status of not existing problem" {
        client.getProblemState("not-existing-problem") shouldBe BacsProblemState.NOT_FOUND
    }
})