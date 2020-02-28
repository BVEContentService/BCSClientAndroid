//  This file is part of BVE Content Service Client (BCSC).
//
//  BCSC is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  BCSC is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with BCSC.  If not, see <https://www.gnu.org/licenses/>.

package tk.zbx1425.bvecontentservice.api

import Identification
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata
import tk.zbx1425.bvecontentservice.io.log.Log
import tk.zbx1425.bvecontentservice.replace
import java.io.InputStream

object HttpHelper {
    val client = OkHttpClient()
    val cachedResponse = SimpleStringMap()
    val requestInterceptMap: Map<String, String> = mapOf(
        //Nicely done, CPC. NICELY done.
        "ajax.googleapis.com/ajax/libs/jquery" to "cdn.staticfile.org/jquery",
        "maxcdn.bootstrapcdn.com/bootstrap" to "cdn.staticfile.org/twitter-bootstrap",
        "translate.google.com" to "127.0.0.1"
    )
    const val REFERER = "https://anti-hotlink.zbx1425.tk"
    val deviceUA = System.getProperty("http.agent")
        ?: "Mozilla/5.0 (Linux; Android 4.4.4; SAMSUNG-SM-N900A Build/tt) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/33.0.0.0 Mobile Safari/537.36"

    fun fetchApiArray(source: SourceMetadata, sub: String): JSONArray? {
        return JSONArray(fetchApiString(source, sub) ?: return null)
    }

    fun fetchApiObject(source: SourceMetadata, sub: String): JSONObject? {
        return JSONObject(fetchApiString(source, sub) ?: return null)
    }

    fun fetchApiString(source: SourceMetadata, sub: String): String? {
        val url = if (ManagerConfig.reverseProxy && source.APIRProxy != "") {
                //Log.i("BCSHttpHelper", "Built request with RPROXY "+source.APIRProxy)
            source.APIRProxy.trim() + sub
            } else {
            source.APIURL.trim() + sub
            }
        return fetchString(source, url)
    }

    fun fetchArray(url: String): JSONArray? {
        return JSONArray(fetchString(url) ?: return null)
    }

    fun fetchObject(url: String): JSONObject? {
        return JSONObject(fetchString(url) ?: return null)
    }

    fun fetchString(source: SourceMetadata, url: String): String? {
        if (source.APIType == "httpBasicAuth") {
            if (cachedResponse[source.Username + "@" + url] != null) return cachedResponse[source.Username + "@" + url]
            Log.i("BCSNetwork", "Requested " + source.Username + "@" + url)
        } else {
            if (cachedResponse[url] != null) return cachedResponse[url]
            Log.i("BCSNetwork", "Requested " + url)
        }
        val request = getBasicBuilder(url).build()
        val response = getSourceClient(source).newCall(request).execute()
        return response.body()?.string() ?: return null
    }

    fun fetchString(url: String): String? {
        if (cachedResponse[url] != null) return cachedResponse[url]
        Log.i("BCSNetwork", "Requested " + url)
        val request = getBasicBuilder(url).build()
        val response = client.newCall(request).execute()
        return response.body()?.string() ?: return null
    }

    fun openStream(source: SourceMetadata?, url: String): InputStream? {
        val request: Request = getBasicBuilder(url).build()
        return if (source != null) {
            getSourceClient(source).newCall(request).execute().body()
                ?.byteStream()
        } else {
            client.newCall(request).execute().body()
                ?.byteStream()
        }
    }

    fun getBasicBuilder(url: String, noHack: Boolean = false): Request.Builder {
        return Request.Builder().url(url.replace(requestInterceptMap))
            .header("User-Agent", deviceUA)
            .header("Referer", REFERER)
            .header("X-BCS-UUID", Identification.deviceID)
            .header("X-BCS-CHECKSUM", Identification.getDateChecksum())
    }

    fun shouldInterceptRequest(url: String): Boolean {
        requestInterceptMap.forEach { if (url.contains(it.key)) return true }
        return false
    }

    fun getSourceClient(source: SourceMetadata): OkHttpClient {
        return when (source.APIType) {
            "httpSimple" -> OkHttpClient()
            "httpBasicAuth" -> OkHttpClient.Builder()
                .authenticator { route: Route?, response: Response ->
                    val credential: String = Credentials.basic(source.Username, source.Password)
                    response.request().newBuilder()
                        .header("Authorization", credential).build()
                }.build()
            else -> throw IllegalArgumentException("Bad APIType! " + source.APIType)
        }
    }
}