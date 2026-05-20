package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SoundCloudUser(
    @Json(name = "username") val username: String? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudFormat(
    @Json(name = "protocol") val protocol: String? = null,
    @Json(name = "mime_type") val mimeType: String? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudTranscoding(
    @Json(name = "url") val url: String? = null,
    @Json(name = "preset") val preset: String? = null,
    @Json(name = "format") val format: SoundCloudFormat? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudMedia(
    @Json(name = "transcodings") val transcodings: List<SoundCloudTranscoding>? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudTrackResponse(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "user") val user: SoundCloudUser? = null,
    @Json(name = "artwork_url") val artworkUrl: String? = null,
    @Json(name = "duration") val duration: Long? = null,
    @Json(name = "media") val media: SoundCloudMedia? = null
)

@JsonClass(generateAdapter = true)
data class SoundCloudSearchResponse(
    @Json(name = "collection") val collection: List<SoundCloudTrackResponse>? = null
)

object SoundCloudClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val searchAdapter = moshi.adapter(SoundCloudSearchResponse::class.java)

    @Volatile
    private var cachedClientId: String? = null

    private val fallbacks = listOf(
        "iZ7g2BCmvaSgX6Tsh6n1Z780f2H7S6zG",
        "a3e059563d7eb3372b72540b6f3b7568",
        "kM9v9168W86b0S8y88p0V2sh6x"
    )

    // Scraping the currently valid public client_id dynamically from soundcloud.com scripts
    suspend fun getClientId(): String = withContext(Dispatchers.IO) {
        cachedClientId?.let { return@withContext it }

        try {
            val homeRequest = Request.Builder()
                .url("https://soundcloud.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            
            client.newCall(homeRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    val scriptRegex = java.util.regex.Pattern.compile(
                        "src=\"(https://a-v2\\.sndcdn\\.com/assets/[^\"]+\\.js)\""
                    )
                    val matcher = scriptRegex.matcher(html)
                    val scriptUrls = mutableListOf<String>()
                    while (matcher.find()) {
                        scriptUrls.add(matcher.group(1))
                    }

                    for (scriptUrl in scriptUrls.reversed()) {
                        try {
                            val jsRequest = Request.Builder()
                                .url(scriptUrl)
                                .header("User-Agent", "Mozilla/5.0")
                                .build()
                            client.newCall(jsRequest).execute().use { jsRes ->
                                if (jsRes.isSuccessful) {
                                    val jsCode = jsRes.body?.string() ?: ""
                                    val idPattern = java.util.regex.Pattern.compile("client_id[:\"\\s=]+([a-zA-Z0-9]{32})")
                                    val idMatcher = idPattern.matcher(jsCode)
                                    if (idMatcher.find()) {
                                        val foundId = idMatcher.group(1)
                                        cachedClientId = foundId
                                        return@withContext foundId
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // try next js bundle
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext fallbacks.first()
    }

    suspend fun searchTracks(query: String): List<SoundCloudTrackResponse> = withContext(Dispatchers.IO) {
        val clientId = getClientId()
        val url = "https://api-v2.soundcloud.com/search/tracks?q=${URLEncoder.encode(query, "UTF-8")}&client_id=$clientId&limit=25"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("SoundCloud search failed: HTTP ${response.code}")
            val bodyString = response.body?.string() ?: ""
            val parsed = searchAdapter.fromJson(bodyString)
            parsed?.collection ?: emptyList()
        }
    }

    suspend fun getStreamUrl(trackId: String, transcodingsJson: String): String = withContext(Dispatchers.IO) {
        val clientId = getClientId()
        
        // Transcodings are stored as a JSON array / serialized structure
        val transJson = JSONObject(transcodingsJson)
        val transArray = transJson.optJSONArray("transcodings") ?: throw Exception("No transcodings found")
        
        var selectedUrl: String? = null
        // First try progressive stream
        for (i in 0 until transArray.length()) {
            val transObj = transArray.getJSONObject(i)
            val formatObj = transObj.optJSONObject("format")
            if (formatObj?.optString("protocol") == "progressive") {
                selectedUrl = transObj.optString("url")
                break
            }
        }

        // Fall back to first available if progressive is not found
        if (selectedUrl == null && transArray.length() > 0) {
            selectedUrl = transArray.getJSONObject(0).optString("url")
        }

        val finalMediaUrl = selectedUrl ?: throw Exception("Transcoding URL was missing")
        val streamRequestUrl = "$finalMediaUrl?client_id=$clientId"
        
        val request = Request.Builder()
            .url(streamRequestUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("SoundCloud stream resolve failed: HTTP ${response.code}")
            val bodyString = response.body?.string() ?: ""
            val jsonObject = JSONObject(bodyString)
            jsonObject.getString("url")
        }
    }

    fun getHighResArtworkUrl(artworkUrl: String?): String? {
        if (artworkUrl == null) return null
        return if (artworkUrl.contains("-large.")) {
            artworkUrl.replace("-large.", "-t500x500.")
        } else {
            artworkUrl
        }
    }
}
