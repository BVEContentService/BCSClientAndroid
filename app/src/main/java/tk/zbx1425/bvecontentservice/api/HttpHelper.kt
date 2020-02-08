package tk.zbx1425.bvecontentservice.api

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject

object HttpHelper {
    val client = HttpClientFactory.getHttpClient()!!

    fun fetchArray(source: SourceMetadata, sub: String): JSONArray? {
        return JSONArray(fetchString(source, sub) ?: return null)
    }

    fun fetchObject(source: SourceMetadata, sub: String): JSONObject? {
        return JSONObject(fetchString(source, sub) ?: return null)
    }

    fun fetchString(source: SourceMetadata, sub: String): String? {
        val request = Request.Builder()
            .url(source.APIURL.trim() + sub)
            .build()
        val response = getSourceClient(source).newCall(request).execute()
        return response.body()?.string() ?: return null
    }

    fun fetchNonapiString(source: SourceMetadata, url: String): String? {
        val request = Request.Builder()
            .url(url)
            .build()
        val response = getSourceClient(source).newCall(request).execute()
        return response.body()?.string() ?: return null
    }

    fun getSourceClient(source: SourceMetadata): OkHttpClient {
        return when (source.APIType) {
            "httpSimple" -> OkHttpClient()
            "httpBasicAuth" -> OkHttpClient.Builder()
                .authenticator { route: Route, response: Response ->
                    val credential: String = Credentials.basic(source.Username, source.Password)
                    response.request().newBuilder()
                        .header("Authorization", credential).build()
                }.build()
            else -> throw IllegalArgumentException("Bad APIType! " + source.APIType)
        }
    }
}