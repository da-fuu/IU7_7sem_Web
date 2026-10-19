package com.ppo.data.api

import java.io.File
import java.net.HttpURLConnection
import java.net.URI

class RestApiClient(
    val baseUrl: String,
    private val tokenFile: File
) {
    data class Response(
        val statusCode: Int,
        val body: String
    )

    private fun getToken(): String? {
        val jwtFile = File(tokenFile.parentFile, "jwt_token.txt")
        return if (jwtFile.exists()) jwtFile.readText().trim() else null
    }

    fun saveToken(token: String) {
        val jwtFile = File(tokenFile.parentFile, "jwt_token.txt")
        jwtFile.writeText(token)
    }

    fun clearToken() {
        val jwtFile = File(tokenFile.parentFile, "jwt_token.txt")
        if (jwtFile.exists()) jwtFile.delete()
    }

    fun request(
        method: String,
        endpoint: String,
        jsonBody: String? = null
    ): Response {
        val normalizedBase = if (baseUrl.endsWith("/")) baseUrl.dropLast(1) else baseUrl
        val normalizedEndpoint = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
        val fullUrl = "$normalizedBase$normalizedEndpoint"

        val connection = URI(fullUrl).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val token = getToken()
        if (token != null) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }

        if (jsonBody != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
            connection.doOutput = true
            connection.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return Response(code, responseBody)
    }
}
