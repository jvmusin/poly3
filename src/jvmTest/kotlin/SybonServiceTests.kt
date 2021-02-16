import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import sybon.SybonService
import sybon.sybonModule
import util.retrofitModule

class SybonServiceTests : StringSpec({
    val service by inject(SybonService::class.java)

    "Get problem by bacs problem id" {
        val problem = service.getProblemByBacsProblemId(A_PLUS_B_BACS_PROBLEM_ID)
        problem.shouldNotBeNull()
        problem.internalProblemId shouldBe A_PLUS_B_BACS_PROBLEM_ID
        problem.id shouldBe A_PLUS_B_SYBON_PROBLEM_ID
    }
}), KoinTest {
    companion object {
        const val A_PLUS_B_BACS_PROBLEM_ID = "1000pre"
        const val A_PLUS_B_SYBON_PROBLEM_ID = 8138
    }

    override fun listeners() = listOf(KoinListener(sybonModule + retrofitModule))
}