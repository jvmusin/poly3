@file:OptIn(ExperimentalTime::class, ExperimentalPathApi::class, ExperimentalCoroutinesApi::class)

package server.routes

import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.Route
import io.ktor.routing.route
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.io.path.ExperimentalPathApi
import kotlin.time.ExperimentalTime

fun Route.rootRoute() {
    static("static") { resources() }
    homeRoute()
    notificationsRoute()
    secretsRoute()
    route("problems") {
        problemsRoute()
    }
}
