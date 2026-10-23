
package com.example

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.delete
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        )
    }

    // Middleware: logging of HTTP requests
    install(CallLogging) {
        level = Level.INFO
    }

    // Centralized exception handling
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    code = "invalid_json",
                    message = "Request body is not valid JSON",
                    details = cause.message
                )
            )
        }

        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError(
                    code = "bad_request",
                    message = cause.message ?: "Invalid request"
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.application.environment.log.error(
                "Unhandled request error",
                cause
            )

            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError(
                    code = "internal_error",
                    message = "An unexpected error occurred"
                )
            )
        }
    }

    routing {
        get("/") {
            call.respond(
                mapOf(
                    "name" to "Ktor Tasks API",
                    "docs" to "/swagger",
                    "openapi" to "/openapi.yaml"
                )
            )
        }

        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        // Swagger UI
        get("/swagger") {
            call.respondText(
                """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8"/>
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1"/>
                    <title>Ktor Tasks API — Swagger UI</title>
                    <link rel="stylesheet"
                          href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css"/>
                </head>
                <body>
                    <div id="swagger-ui"></div>
                    <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                    <script>
                        SwaggerUIBundle({
                            url: "/openapi.yaml",
                            dom_id: "#swagger-ui"
                        });
                    </script>
                </body>
                </html>
                """.trimIndent(),
                ContentType.Text.Html
            )
        }

        // OpenAPI specification
        get("/openapi.yaml") {
            val resource =
                this::class.java.classLoader.getResource("openapi.yaml")

            if (resource == null) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiError(
                        code = "spec_missing",
                        message = "OpenAPI specification is missing"
                    )
                )
            } else {
                call.respondText(
                    resource.readText(),
                    ContentType.parse("application/yaml")
                )
            }
        }

        // Tasks API
        route("/tasks") {
            // GET /tasks
            get {
                call.respond(TaskStore.all())
            }

            // GET /tasks/{id}
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException(
                        "Task id must be an integer"
                    )

                val task = TaskStore.find(id)

                if (task == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiError(
                            code = "task_not_found",
                            message = "Task $id was not found"
                        )
                    )
                } else {
                    call.respond(task)
                }
            }

            // POST /tasks
            post {
                val request = call.receive<CreateTaskRequest>()
                val title = request.title.trim()

                if (title.isEmpty()) {
                    throw IllegalArgumentException(
                        "title must not be blank"
                    )
                }

                if (title.length > 120) {
                    throw IllegalArgumentException(
                        "title must be at most 120 characters"
                    )
                }

                call.respond(
                    HttpStatusCode.Created,
                    TaskStore.create(title)
                )
            }

            // DELETE /tasks/{id}
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw IllegalArgumentException(
                        "Task id must be an integer"
                    )

                if (TaskStore.delete(id)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiError(
                            code = "task_not_found",
                            message = "Task $id was not found"
                        )
                    )
                }
            }
        }
    }
}
