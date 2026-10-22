package com.example.archvm.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class HttpFetcher {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchUrl(url: String, includeHeaders: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        try {
            val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else url

            val request = Request.Builder()
                .url(formattedUrl)
                .header("User-Agent", "curl/8.10.1 (x86_64-pc-linux-gnu) ArchVM")
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val message = response.message
            val body = response.body?.string() ?: ""

            if (includeHeaders) {
                val headers = response.headers
                val headerStr = StringBuilder("HTTP/2 $code $message\n")
                for (i in 0 until headers.size) {
                    headerStr.append("${headers.name(i)}: ${headers.value(i)}\n")
                }
                headerStr.append("\n").append(body.take(1500))
                Result.success(headerStr.toString())
            } else {
                Result.success(body.take(4000))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
