package bacs

import bacs.BacsProblemState.IMPORTED
import bacs.BacsProblemState.NOT_FOUND
import bacsModule
import io.kotest.assertions.retry
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.shouldBe
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.time.minutes
import kotlin.time.seconds

class BacsArchiveServiceTests : BehaviorSpec(), KoinTest {
    override fun listeners() = listOf(KoinListener(bacsModule))
    private val service: BacsArchiveService by inject()

    init {
        Given("getProblemState") {
            When("problem is imported correctly") {
                Then("returns status IMPORTED") {
                    retry(3, 5.minutes, 3.seconds) {
                        service.getProblemState("polybacs-frog-and-polygon-ok") shouldBe IMPORTED
                    }
                }
            }
            When("problem does not exist") {
                Then("returns status NOT_FOUND") {
                    retry(3, 5.minutes, 3.seconds) {
                        service.getProblemState("not-existing-problem") shouldBe NOT_FOUND
                    }
                }
            }
        }
    }
}
