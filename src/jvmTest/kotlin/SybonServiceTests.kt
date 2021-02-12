import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import sybon.SybonApiFactory
import sybon.SybonServiceFactory

class SybonServiceTests : StringSpec({
    val service = SybonServiceFactory(SybonApiFactory()).create()

    "Get problem by bacs problem id" {
        val problem = service.getProblemByBacsProblemId(A_PLUS_B_BACS_PROBLEM_ID)
        problem.shouldNotBeNull()
        problem.internalProblemId shouldBe A_PLUS_B_BACS_PROBLEM_ID
        problem.id shouldBe A_PLUS_B_SYBON_PROBLEM_ID
    }
}) {
    companion object {
        const val A_PLUS_B_BACS_PROBLEM_ID = "1000pre"
        const val A_PLUS_B_SYBON_PROBLEM_ID = 8138
    }
}