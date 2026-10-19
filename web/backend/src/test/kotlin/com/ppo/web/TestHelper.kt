package com.ppo.web

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder

fun ApplicationTestBuilder.setupTestApp(routes: Route.() -> Unit): HttpClient {
    application {
        this.install(ServerContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                registerKotlinModule()
            }
        }
        this.routing {
            routes()
        }
    }
    return createClient {
        this.install(ClientContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                registerKotlinModule()
            }
        }
    }
}
