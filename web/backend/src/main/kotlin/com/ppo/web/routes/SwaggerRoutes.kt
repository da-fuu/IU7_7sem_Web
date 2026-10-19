package com.ppo.web.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

fun Route.swaggerRoutes(specFile: File) {
    get("/health") {
        call.respondText("""{"status":"UP"}""", ContentType.Application.Json, HttpStatusCode.OK)
    }

    get("/api/v1/openapi.yaml") {
        if (specFile.exists()) {
            call.respondText(specFile.readText(), ContentType.parse("text/yaml; charset=utf-8"), HttpStatusCode.OK)
        } else {
            val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("openapi.yaml")
            if (resourceStream != null) {
                val text = resourceStream.bufferedReader().use { it.readText() }
                call.respondText(text, ContentType.parse("text/yaml; charset=utf-8"), HttpStatusCode.OK)
            } else {
                call.respondError(HttpStatusCode.NotFound, "NOT_FOUND", "Спецификация OpenAPI не найдена")
            }
        }
    }

    get("/api/v1/docs") {
        val html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>TimeTracker REST API - Swagger UI</title>
                <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.18.2/swagger-ui.css">
                <style>
                    html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }
                    *, *:before, *:after { box-sizing: inherit; }
                    body { margin:0; background: #fafafa; }
                </style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5.18.2/swagger-ui-bundle.js"></script>
                <script src="https://unpkg.com/swagger-ui-dist@5.18.2/swagger-ui-standalone-preset.js"></script>
                <script>
                window.onload = function() {
                    const ui = SwaggerUIBundle({
                        url: "/api/v1/openapi.yaml",
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [
                            SwaggerUIBundle.presets.apis,
                            SwaggerUIStandalonePreset
                        ],
                        plugins: [
                            SwaggerUIBundle.plugins.DownloadUrl
                        ],
                        layout: "StandaloneLayout"
                    });
                    window.ui = ui;
                };
                </script>
            </body>
            </html>
        """.trimIndent()
        call.respondText(html, ContentType.Text.Html, HttpStatusCode.OK)
    }
}
