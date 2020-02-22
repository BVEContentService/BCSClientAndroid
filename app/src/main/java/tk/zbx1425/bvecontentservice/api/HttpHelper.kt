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

object HttpHelper {
    val client = OkHttpClient()
    val cachedResponse = HashMap<String, String>()

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
        if (cachedResponse[url] != null) return cachedResponse[url]
        val request = Request.Builder()
            .url(url)
            .addHeader("Referer", "https://anti-hotlink.zbx1425.tk")
            .addHeader("X-BCS-UUID", Identification.deviceID)
            .addHeader("X-BCS-CHECKSUM", Identification.getDateChecksum())
            .build()
        val response = getSourceClient(source).newCall(request).execute()
        return response.body()?.string() ?: return null
    }

    fun fetchString(url: String): String? {
        if (cachedResponse[url] != null) return cachedResponse[url]
        val request = Request.Builder()
            .url(url)
            .addHeader("Referer", "https://anti-hotlink.zbx1425.tk")
            .addHeader("X-BCS-UUID", Identification.deviceID)
            .addHeader("X-BCS-CHECKSUM", Identification.getDateChecksum())
            .build()
        val response = client.newCall(request).execute()
        return response.body()?.string() ?: return null
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