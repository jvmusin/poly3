package sybon

import api.AdditionalProblemProperties
import bacs.BacsArchiveService
import bacsModule
import io.kotest.core.spec.style.StringSpec
import io.kotest.koin.KoinListener
import org.koin.java.KoinJavaComponent.inject
import org.koin.test.KoinTest
import polygon.PolygonService
import polygonModule
import sybon.api.SybonArchiveApi
import sybonModule
import java.time.LocalDateTime
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SybonSpecialCollectionTests :
    StringSpec({
        val bacsArchiveService by inject(BacsArchiveService::class.java)
        val sybonArchiveApi by inject(SybonArchiveApi::class.java)
        val polygonService by inject(PolygonService::class.java)
        val specialCollectionId = 10023
        val polygonProblemId = 147360
        val properties = AdditionalProblemProperties(suffix = LocalDateTime.now().run { "-$hour-$minute" })

        "!Full cycle" {
            val irProblem = polygonService.downloadProblem(polygonProblemId)
            val fullName = bacsArchiveService.uploadProblem(irProblem, properties)
            val sybonProblemId = sybonArchiveApi.importProblem(specialCollectionId, fullName)
            println(fullName)
            println(sybonProblemId)
            println(sybonArchiveApi.getCollection(specialCollectionId))
        }

        "Print special collection" {
            println(sybonArchiveApi.getCollection(specialCollectionId))
        }
    }),
    KoinTest {
    override fun listeners() = listOf(KoinListener(polygonModule + bacsModule + sybonModule))
}
