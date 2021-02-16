import api.AdditionalProblemProperties
import bacs.BacsArchiveService
import bacs.BacsProblemState
import bacs.bacsModule
import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import io.kotest.matchers.shouldBe
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import polygon.PolygonProblemDownloader
import polygon.polygonModule
import sybon.SybonArchiveBuilder
import sybon.api.SybonArchiveApi
import sybon.sybonModule
import util.retrofitModule
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

@OptIn(ExperimentalTime::class)
class SybonSpecialCollectionTests : StringSpec({
    val bacsArchiveService by inject(BacsArchiveService::class.java)
    val sybonArchiveApi by inject(SybonArchiveApi::class.java)
    val problemDownloader by inject(PolygonProblemDownloader::class.java)
    val archiveBuilder by inject(SybonArchiveBuilder::class.java)
    val specialCollectionId = 10023
    val polygonProblemId = 147360
    val properties = AdditionalProblemProperties(suffix = LocalDateTime.now().run { "-$hour-$minute" })

    "!Full cycle" {
        val irProblem = problemDownloader.download(polygonProblemId, false)
        val fullName = properties.buildFullName(irProblem.name)
        println(fullName)
        val zip = archiveBuilder.build(irProblem, properties)
        bacsArchiveService.uploadProblem(zip)
        bacsArchiveService.waitTillProblemIsImported(fullName, 1.minutes).state shouldBe BacsProblemState.IMPORTED
        val sybonProblemId = sybonArchiveApi.postProblem(specialCollectionId, fullName)
        println(sybonProblemId)
        println(sybonArchiveApi.getCollection(specialCollectionId))
    }

    "Print special collection" {
        println(sybonArchiveApi.getCollection(specialCollectionId))
    }

}), KoinTest {
    override fun listeners() = listOf(KoinListener(retrofitModule + polygonModule + bacsModule + sybonModule))
}