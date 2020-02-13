package tk.zbx1425.bvecontentservice.api

import androidx.preference.PreferenceManager
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata

object HttpHelper {
    val client = OkHttpClient()

    fun fetchArray(source: SourceMetadata, sub: String): JSONArray? {
        return JSONArray(fetchString(source, sub) ?: return null)
    }

    fun fetchObject(source: SourceMetadata, sub: String): JSONObject? {
        return JSONObject(fetchString(source, sub) ?: return null)
    }

    fun fetchString(source: SourceMetadata, sub: String): String? {
        val request =
            if (PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context).getBoolean(
                    "reverseProxy", false
                ) && source.APIRProxy != ""
            ) {
                //Log.i("BCSHttpHelper", "Built request with RPROXY "+source.APIRProxy)
                Request.Builder().url(source.APIRProxy.trim() + sub).build()
            } else {
                Request.Builder().url(source.APIURL.trim() + sub).build()
            }
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