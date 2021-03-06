package bacs

import bacs.BacsProblemState.IMPORTED
import bacs.BacsProblemState.NOT_FOUND
import bacsModule
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.shouldBe
import org.koin.test.KoinTest
import org.koin.test.inject

class BacsArchiveServiceTests : BehaviorSpec(), KoinTest {
    override fun listeners() = listOf(KoinListener(bacsModule))
    private val service: BacsArchiveService by inject()

    init {
        Given("getProblemState") {
            When("problem is imported correctly") {
                Then("returns status IMPORTED") {
                    service.getProblemState("polybacs-frog-and-polygon-ok") shouldBe IMPORTED
                }
            }
            When("problem does not exist") {
                Then("returns status NOT_FOUND") {
                    service.getProblemState("not-existing-problem") shouldBe NOT_FOUND
                }
            }
        }
    }
}
