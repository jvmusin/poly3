@file:OptIn(ExperimentalPathApi::class, ExperimentalTime::class)

package server.routes

import api.NameAvailability
import bacs.BacsArchiveService
import bacs.BacsProblemState
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import org.koin.ktor.ext.inject
import polygon.PolygonService
import polygon.api.toDto
import kotlin.io.path.ExperimentalPathApi
import kotlin.time.ExperimentalTime

fun Route.problemsRoute() {
    val bacsArchiveService: BacsArchiveService by inject()
    val polygonService: PolygonService by inject()

    get {
        val problems = polygonService.getProblems()
        call.respond(HttpStatusCode.OK, problems.map { it.toDto() })
    }
    get("get-name-availability") {
        val name = call.parameters["name"]!!
        val availability = when (bacsArchiveService.getProblemState(name)) {
            BacsProblemState.NOT_FOUND -> NameAvailability.AVAILABLE
            BacsProblemState.IMPORTED, BacsProblemState.PENDING_IMPORT -> NameAvailability.TAKEN
            BacsProblemState.UNKNOWN -> NameAvailability.CHECK_FAILED
        }
        call.respond(HttpStatusCode.OK, availability)
    }
    route("{problem-id}") {
        problemRoute()
    }
}
