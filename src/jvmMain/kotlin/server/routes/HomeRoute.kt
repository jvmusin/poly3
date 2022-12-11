package server.routes

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.system.exitProcess

@Suppress("SpellCheckingInspection")
private val index = """
    <!doctype html>
    <html lang="en">
      <head>
        <!-- Required meta tags -->
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <!-- Bootstrap CSS -->
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-BmbxuPwQa2lc/FVzBcNJ7UAyJxM6wuqIj61tLrc4wSX0szH/Ev+nYRRuWlolflfl" crossorigin="anonymous">
        
        <!-- Bootstrap Icons -->
        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.3.0/font/bootstrap-icons.css">

        <title>Полибакс!!</title>
      </head>
      <body>
        <!-- Main content -->
        <div id="root"></div>
        <!-- JavaScript Bundle with Popper -->
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/js/bootstrap.bundle.min.js" integrity="sha384-b5kHyXgcpbZJO/tY9Ul7kGkf1S0CWuKcCD38l8YkeH8z8QjE0GmW1gYU5S9FOnJ0" crossorigin="anonymous"></script>
        <!-- Main script -->
        <script src="/static/poly3.js"></script>
      </body>
    </html>
""".trimIndent()

private val noSleepDuration = Duration.ofMinutes(30)
private val sleepAt = AtomicReference(Instant.now() + noSleepDuration)

fun Route.homeRoute() {
    thread(start = true) {
        while (true) {
            val now = Instant.now()
            val sleep = sleepAt.get()
            if (now > sleep) {
                println("Going to shutdown because it's time")
                exitProcess(0)
            } else {
                val addSleep = Duration.between(now, sleep)
                println("Not going to shutdown, sleep now for $addSleep")
                Thread.sleep(addSleep.toMillis() + 10_000)
            }
        }
    }
    get {
        sleepAt.getAndUpdate { old -> old + noSleepDuration }
        call.respondText(index, ContentType.Text.Html)
    }
}
