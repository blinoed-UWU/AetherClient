package dev.aetherclient.launcher.core.network

import dev.aetherclient.launcher.core.platform.HashUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object HttpClientFactory {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
            defaultRequest {
                header(HttpHeaders.UserAgent, "AetherLauncher/1.0.0 (Minecraft 1.21.11 Fabric)")
            }
        }
    }

    suspend fun downloadFileIfNeeded(
        url: String,
        target: Path,
        expectedSha1: String? = null,
        expectedSha256: String? = null
    ) = withContext(Dispatchers.IO) {
        if (Files.isRegularFile(target)) {
            val valid = when {
                expectedSha256 != null -> HashUtils.sha256(target).equals(expectedSha256, ignoreCase = true)
                expectedSha1 != null -> HashUtils.sha1(target).equals(expectedSha1, ignoreCase = true)
                else -> Files.size(target) > 0L
            }
            if (valid) return@withContext
        }

        Files.createDirectories(target.parent)
        val tmpFile = target.resolveSibling("${target.fileName}.part")

        val response = client.get(url)
        val channel = response.bodyAsChannel()
        Files.newOutputStream(tmpFile).use { out ->
            channel.copyTo(out)
        }

        if (expectedSha1 != null) {
            val actual = HashUtils.sha1(tmpFile)
            if (!actual.equals(expectedSha1, ignoreCase = true)) {
                Files.deleteIfExists(tmpFile)
                error("SHA-1 mismatch for ${target.fileName}: expected $expectedSha1, got $actual")
            }
        }
        if (expectedSha256 != null) {
            val actual = HashUtils.sha256(tmpFile)
            if (!actual.equals(expectedSha256, ignoreCase = true)) {
                Files.deleteIfExists(tmpFile)
                error("SHA-256 mismatch for ${target.fileName}: expected $expectedSha256, got $actual")
            }
        }

        Files.move(tmpFile, target, StandardCopyOption.REPLACE_EXISTING)
    }

    suspend inline fun <reified T> getJson(url: String): T = client.get(url).body()
}
