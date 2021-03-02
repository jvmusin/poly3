package sybon.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SybonCheckingApi {
    @GET("Compilers")
    suspend fun getCompilers(): List<SybonCompiler>

    @POST("Submits/send")
    suspend fun submitSolution(@Body solution: SybonSubmitSolution): Int

    @POST("Submits/sendall")
    suspend fun submitSolutions(@Body solutions: List<SybonSubmitSolution>): List<Int>

    @POST("Submits/rejudge")
    suspend fun rejudge(@Body ids: List<Int>)

    @GET("Submits/results")
    suspend fun getResults(@Query("ids") ids: String): List<SybonSubmissionResult>
}
