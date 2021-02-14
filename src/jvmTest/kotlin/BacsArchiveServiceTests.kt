import bacs.BacsArchiveServiceFactory
import bacs.BacsProblemState
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BacsArchiveServiceTests : StringSpec({
    val client = BacsArchiveServiceFactory().create()

    "Get status of correctly imported problem" {
        client.getProblemStatus("polybacs-frog-and-polygon-ok").state shouldBe BacsProblemState.IMPORTED
    }

    "Get status of not existing problem" {
        client.getProblemStatus("not-existing-problem").state shouldBe BacsProblemState.NOT_FOUND
    }
})