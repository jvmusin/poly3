package server.routes

import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.secretsRoute() {
    get("/secrets") {
        val load = ConfigFactory.load()
        print(load)
        val map = ConfigFactory.load().entrySet().map { it.key to it.value.render() }
        require(map.map { it.first }.toSet().size == map.size) // Assure no duplicates
        call.respond(map.toMap())
    }
}