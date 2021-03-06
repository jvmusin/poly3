package sybon

import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import sybonModule

class SybonServiceTests :
    StringSpec({
        val service by inject(SybonArchiveService::class.java, TestProblemArchive)

        "Import problem" {
            val problem = service.importProblem(A_PLUS_B_BACS_PROBLEM_ID)
            problem.shouldNotBeNull()
            problem.id shouldBe A_PLUS_B_SYBON_PROBLEM_ID
            problem.internalProblemId shouldBe A_PLUS_B_BACS_PROBLEM_ID
        }
    }),
    KoinTest {
    companion object {
        const val A_PLUS_B_BACS_PROBLEM_ID = "1000pre"
        const val A_PLUS_B_SYBON_PROBLEM_ID = 82120
    }

    override fun listeners() = listOf(KoinListener(sybonModule))
}
