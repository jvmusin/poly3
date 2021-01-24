package polygon

import okhttp3.ResponseBody
import retrofit2.http.POST
import retrofit2.http.Query

const val DEFAULT_TESTSET = "tests"

interface ProblemApi {
    @POST("problems.list")
    suspend fun getProblems(
        @Query("showDeleted") showDeleted: Boolean = false,
        @Query("id") id: Int? = null,
        @Query("name") name: String? = null,
        @Query("owner") owner: String? = null
    ): CodeforcesResponse<List<Problem>>

    @POST("problem.info")
    suspend fun getInfo(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<ProblemInfo>

    @POST("problem.statements")
    suspend fun getStatements(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<Map<String, Statement>>

    @POST("problem.statementResources")
    suspend fun getStatementResources(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<List<File>>

    @POST("problem.checker")
    suspend fun getCheckerName(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.validator")
    suspend fun getValidatorName(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.interactor")
    suspend fun getInteractorName(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.files")
    suspend fun getFiles(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<Map<String, List<File>>>

    @POST("problem.solutions")
    suspend fun getSolutions(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<List<Solution>>

    @POST("problem.viewFile")
    suspend fun getFile(
        @Query("problemId") problemId: Int,
        @Query("type") type: File.Type,
        @Query("name") name: String
    ): ResponseBody

    @POST("problem.viewSolution")
    suspend fun getSolution(
        @Query("problemId") problemId: Int,
        @Query("name") name: String
    ): ResponseBody

    @POST("problem.script")
    suspend fun getScript(
        @Query("problemId") problemId: Int,
        @Query("testset") name: String
    ): ResponseBody

    @POST("problem.tests")
    suspend fun getTests(
        @Query("problemId") problemId: Int,
        @Query("testset") testSet: String = DEFAULT_TESTSET
    ): CodeforcesResponse<List<Test>>

    @POST("problem.testInput")
    suspend fun getTestInput(
        @Query("problemId") problemId: Int,
        @Query("testset") testSet: String = DEFAULT_TESTSET,
        @Query("testIndex") testIndex: Int
    ): String

    @POST("problem.testAnswer")
    suspend fun getTestAnswer(
        @Query("problemId") problemId: Int,
        @Query("testset") testSet: String = DEFAULT_TESTSET,
        @Query("testIndex") testIndex: Int
    ): String

    @POST("problem.viewTestGroup")
    suspend fun getTestGroup(
        @Query("problemId") problemId: Int,
        @Query("testset") testset: String = DEFAULT_TESTSET,
        @Query("group") group: String
    ): CodeforcesResponse<List<TestGroup>>

    @POST("problem.viewTags")
    suspend fun getTags(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<List<String>>

    @POST("problem.viewGeneralDescription")
    suspend fun getGeneralDescription(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.viewGeneralTutorial")
    suspend fun getGeneralTutorial(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<String>

    @POST("problem.packages")
    suspend fun getPackages(
        @Query("problemId") problemId: Int
    ): CodeforcesResponse<List<Package>>

    @POST("problem.package")
    suspend fun getPackage(
        @Query("problemId") problemId: Int,
        @Query("packageId") packageId: Int
    ): ResponseBody
}

interface ContestApi {
    @POST("contest.problems")
    suspend fun getProblems(
        @Query("contestId") contestId: Int
    ): CodeforcesResponse<List<Problem>>
}

interface PolygonApi {
    val problem: ProblemApi
    val contest: ContestApi
}
