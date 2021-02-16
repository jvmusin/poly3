@file:OptIn(ExperimentalTime::class, ExperimentalPathApi::class, ExperimentalCoroutinesApi::class)

package server.routes

import io.ktor.http.content.*
import io.ktor.routing.*
import kotlinx.coroutines.*
import server.routes.problems.problems
import kotlin.io.path.ExperimentalPathApi
import kotlin.time.ExperimentalTime

fun Route.routes() {
    static("static") { resources() }
    home()
    notifications()
    route("problems") {
        problems()
    }
}