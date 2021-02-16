package sybon.api

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SybonArchiveApi {

    @GET("Collections")
    suspend fun getCollections(
        @Query("Offset") offset: Int = 0,
        @Query("Limit") limit: Int = 100
    ): List<SybonCollection>

    @GET("Collections/{collectionId}")
    suspend fun getCollection(@Path("collectionId") collectionId: Int): SybonCollection

    @GET("Problems/{problemId}")
    suspend fun getProblem(@Path("problemId") problemId: Int): SybonProblem

    @GET("Problems/{problemId}/statement")
    suspend fun getProblemStatementUrl(@Path("problemId") problemId: Int): String

    @POST("Collections/{collectionId}/problems")
    suspend fun importProblem(
        @Path("collectionId") collectionId: Int,
        @Query("internalProblemId") internalProblemId: String
    ): Int
}