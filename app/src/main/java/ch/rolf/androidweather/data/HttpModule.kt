package ch.rolf.androidweather.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

val AppJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    install(UserAgent) { agent = "Wetter/1.0 (ch.rolf.androidweather)" }
    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 12_000
        socketTimeoutMillis = 20_000
    }
    install(ContentNegotiation) { json(AppJson) }
    engine {
        config {
            connectTimeout(12, TimeUnit.SECONDS)
            readTimeout(20, TimeUnit.SECONDS)
        }
    }
}
