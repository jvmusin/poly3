package bacs

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface BacsArchiveApi {
    @POST("upload")
    suspend fun uploadProblem(@Body body: RequestBody): String
}