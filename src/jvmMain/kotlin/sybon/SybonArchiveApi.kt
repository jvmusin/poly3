package sybon

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SybonArchiveApi {

    @GET("Collections")
    suspend fun getCollections(
        @Query("Offset") offset: Int = 0,
        @Query("Limit") limit: Int = 100
    ): List<Collection>

    @GET("Collections/{collectionId}")
    suspend fun getCollection(@Path("collectionId") collectionId: Int): Collection

    @GET("Problems/{problemId}")
    suspend fun getProblem(@Path("problemId") problemId: Int): Problem

    @GET("Problems/{problemId}/statement")
    suspend fun getProblemStatementUrl(@Path("problemId") problemId: Int): String
}