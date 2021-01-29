import bacs.BacsArchiveApi
import bacs.uploadProblem
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import sybon.SybonApiFactory
import java.nio.file.Paths

@Ignored
class TempBacsArchiveTests : StringSpec({
    val api = SybonApiFactory().createArchiveApi()

    "main collection problems should contains some problems with author 'Musin'" {
        val problems = api.getCollection(1).problems
        val myProblems = problems.filter { it.author.contains("Musin", ignoreCase = true) }
        println(myProblems.size)
        myProblems.map { "${it.id} ${it.internalProblemId}" }.forEach(::println)
    }

    "send problem to bacs archive" {
        val client = OkHttpClient().newBuilder().addInterceptor(HttpLoggingInterceptor {
            println(it)
        }.setLevel(HttpLoggingInterceptor.Level.HEADERS))
            .addInterceptor {
                it.proceed(it.request().newBuilder().header("Authorization", "Basic c3lib246d2poJDQyZHMwOQ==").build())
            }
            .build()
        val api = Retrofit.Builder()
            .baseUrl("https://archive.bacs.cs.istu.ru/repository/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(Json { isLenient = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create<BacsArchiveApi>()
        val path = Paths.get("4-values-sum-0-low-tl-package174473.zip")
        println(api.uploadProblem(path))
    }
})