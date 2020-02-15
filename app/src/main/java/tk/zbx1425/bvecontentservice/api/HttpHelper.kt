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

import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
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
            if (ManagerConfig.reverseProxy && source.APIRProxy != "") {
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