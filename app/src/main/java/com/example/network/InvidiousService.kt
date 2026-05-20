package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class InvidiousSearchResult(
    @Json(name = "title") val title: String? = null,
    @Json(name = "videoId") val videoId: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "lengthSeconds") val lengthSeconds: Long? = null
)

@JsonClass(generateAdapter = true)
data class InvidiousAdaptiveFormat(
    @Json(name = "type") val type: String? = null,
    @Json(name = "url") val url: String? = null,
    @Json(name = "bitrate") val bitrate: String? = null,
    @Json(name = "container") val container: String? = null
)

@JsonClass(generateAdapter = true)
data class InvidiousVideoDetails(
    @Json(name = "title") val title: String? = null,
    @Json(name = "videoId") val videoId: String? = null,
    @Json(name = "author") val author: String? = null,
    @Json(name = "adaptiveFormats") val adaptiveFormats: List<InvidiousAdaptiveFormat>? = null,
    @Json(name = "formatStreams") val formatStreams: List<InvidiousAdaptiveFormat>? = null
)

interface InvidiousApi {
    @GET
    suspend fun search(
        @Url url: String,
        @Query("q") query: String,
        @Query("type") type: String,
        @Query("region") region: String
    ): List<InvidiousSearchResult>

    @GET
    suspend fun getVideoDetails(
        @Url url: String
    ): InvidiousVideoDetails
}

object InvidiousClient {
    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.thepixora.com",
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://invidious.f5.si",
        "https://yt.chocolatemoo53.com"
    )

    private var currentInstanceIndex = 0

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://inv.thepixora.com/") // Placeholder base
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: InvidiousApi = retrofit.create(InvidiousApi::class.java)

    suspend fun <T> runWithFallback(block: suspend (String) -> T): T {
        var lastException: Exception? = null
        for (i in 0 until INVIDIOUS_INSTANCES.size) {
            val index = (currentInstanceIndex + i) % INVIDIOUS_INSTANCES.size
            val instance = INVIDIOUS_INSTANCES[index]
            try {
                val result = block(instance)
                currentInstanceIndex = index // persist last successful instance
                return result
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException ?: Exception("All public API backends selected currently are offline.")
    }

    suspend fun searchTracks(query: String): List<InvidiousSearchResult> {
        return runWithFallback { instance ->
            val endpoint = "$instance/api/v1/search"
            api.search(url = endpoint, query = query, type = "video", region = "US")
        }
    }

    suspend fun getStreamUrl(videoId: String): String {
        return runWithFallback { instance ->
            val endpoint = "$instance/api/v1/videos/$videoId"
            val details = api.getVideoDetails(url = endpoint)
            
            // Try adaptiveFormats first (audio-only streams)
            val adaptiveFormats = details.adaptiveFormats
            var audioFormat = adaptiveFormats?.firstOrNull { format ->
                val type = format.type ?: ""
                type.contains("audio/", ignoreCase = true)
            } ?: adaptiveFormats?.firstOrNull { format ->
                val container = format.container ?: ""
                container.contains("m4a", ignoreCase = true) || container.contains("webm", ignoreCase = true)
            }

            // Fall back to formatStreams (video+audio multiplexed stream)
            if (audioFormat == null) {
                val formatStreams = details.formatStreams
                audioFormat = formatStreams?.firstOrNull { format ->
                    val type = format.type ?: ""
                    type.contains("audio/", ignoreCase = true)
                } ?: formatStreams?.firstOrNull { format ->
                    val container = format.container ?: ""
                    container.contains("mp4", ignoreCase = true) || container.contains("webm", ignoreCase = true)
                }
            }
            
            val rawUrl = audioFormat?.url ?: throw Exception("No suitable audio streams parsed in this audio container.")
            if (rawUrl.startsWith("/")) {
                "$instance$rawUrl"
            } else {
                rawUrl
            }
        }
    }
}
