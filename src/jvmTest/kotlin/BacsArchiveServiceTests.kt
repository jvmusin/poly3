import bacs.BacsArchiveServiceFactory
import bacs.BacsProblemStatus
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BacsArchiveServiceTests : StringSpec({
    val client = BacsArchiveServiceFactory().create()

    "Get status of correctly imported problem" {
        client.getProblemStatus("polybacs-frog-and-polygon-ok").state shouldBe BacsProblemStatus.State.IMPORTED
    }

    "Get status of not existing problem" {
        client.getProblemStatus("not-existing-problem").state shouldBe BacsProblemStatus.State.NOT_FOUND
    }
})