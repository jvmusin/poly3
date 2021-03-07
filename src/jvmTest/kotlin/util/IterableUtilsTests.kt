package util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class IterableUtilsTests : BehaviorSpec() {
    init {
        Given("sequentiallyGroupedBy") {
            When("sequence is empty") {
                Then("returns empty list") {
                    emptyList<Int>().sequentiallyGroupedBy { it % 2 }.shouldBeEmpty()
                }
            }
            When("sequence contains one item") {
                Then("returns the item in a group") {
                    listOf(5).sequentiallyGroupedBy { it % 2 } shouldBe listOf(Group(1, listOf(5)))
                }
            }
            When("sequence contains several items with the same group") {
                Then("returns a single group with the items") {
                    listOf(1, 3, 5).sequentiallyGroupedBy { it % 2 } shouldBe listOf(Group(1, listOf(1, 3, 5)))
                }
            }
            When("sequence contains items with different groups") {
                And("those groups are pairwise-distinct") {
                    Then("returns those groups") {
                        val items = listOf(1, 3, 5, 6, 2, 0)
                        val expected = listOf(Group(1, listOf(1, 3, 5)), Group(0, listOf(6, 2, 0)))
                        items.sequentiallyGroupedBy { it % 2 } shouldBe expected
                    }
                }
                And("some groups are split with other items") {
                    Then("returns groups with same key several times") {
                        val items = listOf(1, 3, 5, 6, 3, 7, 2, 6)
                        val expected = listOf(
                            Group(1, listOf(1, 3, 5)),
                            Group(0, listOf(6)),
                            Group(1, listOf(3, 7)),
                            Group(0, listOf(2, 6))
                        )
                        items.sequentiallyGroupedBy { it % 2 } shouldBe expected
                    }
                }
            }
        }
    }
}
