package polygon

import okhttp3.ResponseBody
import retrofit2.http.POST
import retrofit2.http.Query

@Suppress("unused")
interface PolygonApi {

    companion object {
        const val DEFAULT_TESTSET = "tests"
    }

    @POST("problems.list")
    suspend fun getProblems(
        @Query("showDeleted") showDeleted: Boolean = false,
        @Query("id") id: Int? = null,
        @Query("name") name: String? = null,
        @Query("owner") owner: String? = null
    ): PolygonResponse<List<Problem>>

    @POST("problem.info")
    suspend fun getProblemInfo(
        @Query("problemId") problemId: Int
    ): PolygonResponse<ProblemInfo>

    @POST("problem.statements")
    suspend fun getStatements(
        @Query("problemId") problemId: Int
    ): PolygonResponse<Map<String, Statement>>

    @POST("problem.statementResources")
    suspend fun getStatementResources(
        @Query("problemId") problemId: Int
    ): PolygonResponse<List<File>>

    @POST("problem.checker")
    suspend fun getCheckerName(
        @Query("problemId") problemId: Int
    ): PolygonResponse<String>

    @POST("problem.validator")
    suspend fun getValidatorName(
        @Query("problemId") problemId: Int
    ): PolygonResponse<String>

    @POST("problem.interactor")
    suspend fun getInteractorName(
        @Query("problemId") problemId: Int
    ): PolygonResponse<String>

    @POST("problem.files")
    suspend fun getFiles(
        @Query("problemId") problemId: Int
    ): PolygonResponse<Map<String, List<File>>>

    @POST("problem.solutions")
    suspend fun getSolutions(
        @Query("problemId") problemId: Int
    ): PolygonResponse<List<Solution>>

    @POST("problem.viewFile")
    suspend fun getFile(
        @Query("problemId") problemId: Int,
        @Query("type") type: File.Type,
        @Query("name") name: String
    ): ResponseBody

    @POST("problem.viewSolution")
    suspend fun getSolutionContent(
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
    ): PolygonResponse<List<PolygonTest>>

    @POST("problem.testInput")
    suspend fun getTestInput(
        @Query("problemId") problemId: Int,
        @Query("testIndex") testIndex: Int,
        @Query("testset") testSet: String = DEFAULT_TESTSET
    ): String

    @POST("problem.testAnswer")
    suspend fun getTestAnswer(
        @Query("problemId") problemId: Int,
        @Query("testIndex") testIndex: Int,
        @Query("testset") testSet: String = DEFAULT_TESTSET
    ): String

    @POST("problem.viewTestGroup")
    suspend fun getTestGroup(
        @Query("problemId") problemId: Int,
        @Query("group") group: String? = null,
        @Query("testset") testset: String = DEFAULT_TESTSET
    ): PolygonResponse<List<TestGroup>>

    @POST("problem.viewTags")
    suspend fun getTags(
        @Query("problemId") problemId: Int
    ): PolygonResponse<List<String>>

    @POST("problem.viewGeneralDescription")
    suspend fun getGeneralDescription(
        @Query("problemId") problemId: Int
    ): PolygonResponse<String>

    @POST("problem.viewGeneralTutorial")
    suspend fun getGeneralTutorial(
        @Query("problemId") problemId: Int
    ): PolygonResponse<String>

    @POST("problem.packages")
    suspend fun getPackages(
        @Query("problemId") problemId: Int
    ): PolygonResponse<List<Package>>

    @POST("problem.package")
    suspend fun getPackage(
        @Query("problemId") problemId: Int,
        @Query("packageId") packageId: Int
    ): ResponseBody

    @POST("contest.problems")
    suspend fun getContestProblems(
        @Query("contestId") contestId: Int
    ): PolygonResponse<List<Problem>>
}
