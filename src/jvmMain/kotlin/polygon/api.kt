package polygon

import okhttp3.ResponseBody
import retrofit2.http.POST
import retrofit2.http.Query

const val DEFAULT_TESTSET = "testset"

interface PolygonProblemsService {
    @POST("problems.list")
    suspend fun list(): CodeforcesResponse<List<Problem>>
}

interface PolygonProblemService {
    @POST("problem.info")
    suspend fun info(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<ProblemInfo>

    @POST("problem.statements")
    suspend fun statements(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<Map<String, Statement>>

    @POST("problem.statementResources")
    suspend fun statementResources(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<List<File>>

    @POST("problem.checker")
    suspend fun checker(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.validator")
    suspend fun validator(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.interactor")
    suspend fun interactor(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.files")
    suspend fun files(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<Map<String, List<File>>>

    @POST("problem.solutions")
    suspend fun solutions(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<List<Solution>>

    @POST("problem.viewFile")
    suspend fun viewFile(
        @Query("problemId") problemId: Int,
        @Query("type") type: File.Type,
        @Query("name") name: String
    ): ResponseBody

    @POST("problem.viewFile")
    suspend fun viewSolution(
        @Query("problemId") problemId: Int,
        @Query("name") name: String
    ): ResponseBody

    @POST("problem.script")
    suspend fun viewScript(
        @Query("problemId") problemId: Int,
        @Query("testset") name: String
    ): ResponseBody

    @POST("problem.tests")
    suspend fun tests(
        @Query("problemId") problemId: Int,
        @Query("testset") testSet: String = DEFAULT_TESTSET
    ): CodeforcesResponse<List<Test>>

    @POST("problem.testInput")
    suspend fun testInput(
        @Query("problemId") problemId: Int,
        @Query("testset") testSet: String = DEFAULT_TESTSET,
        @Query("testIndex") testIndex: Int
    ): String

    @POST("problem.testAnswer")
    suspend fun testAnswer(
        @Query("problemId") problemId: Int,
        @Query("testset") testSet: String = DEFAULT_TESTSET,
        @Query("testIndex") testIndex: Int
    ): String

    @POST("problem.viewTestGroup")
    suspend fun viewTestGroup(
        @Query("problemId") problemId: Int,
        @Query("testset") testset: String = DEFAULT_TESTSET,
        @Query("group") group: String
    ): CodeforcesResponse<List<TestGroup>>

    @POST("problem.viewTags")
    suspend fun viewTags(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<List<String>>

    @POST("problem.viewGeneralDescription")
    suspend fun viewGeneralDescription(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.viewGeneralTutorial")
    suspend fun viewGeneralTutorial(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.packages")
    suspend fun packages(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<List<Package>>

    @POST("problem.package")
    suspend fun `package`(
        @Query("problemId") problemId: Int,
        @Query("packageId") packageId: Int
    ): ResponseBody
}

interface PolygonContestService {
    @POST("contest.problems")
    suspend fun problems(
        @Query("contestId") contestId: Int
    ): CodeforcesResponse<List<Problem>>
}

class PolygonService(
    val problems: PolygonProblemsService,
    val problem: PolygonProblemService,
    val contest: PolygonContestService
)
