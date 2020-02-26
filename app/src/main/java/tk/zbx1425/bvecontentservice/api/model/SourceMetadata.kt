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

package tk.zbx1425.bvecontentservice.api.model

import org.json.JSONObject
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.chooseString
import tk.zbx1425.bvecontentservice.io.log.Log
import java.io.Serializable

data class SourceMetadata(
    val Name_LO: String,
    val Name_EN: String,
    val APIURL: String,
    val APIRProxy: String,
    val APIType: String,
    val Author_LO: String,
    val Author_EN: String,
    val Homepage: String,
    val Contact: String,
    val Username: String,
    val Password: String,
    val Index: IndexMetadata,
    var DevSpec: DevSpecMetadata
) : Serializable {
    constructor (src: JSONObject, index: IndexMetadata) : this(
        src.getString("Name_LO"),
        src.getString("Name_EN"),
        src.getString("APIURL"),
        src.optString("APIRProxy"),
        src.getString("APIType"),
        src.getString("Author_LO"),
        src.getString("Author_EN"),
        src.optString("Homepage"),
        src.getString("Contact"),
        src.optString("Username"),
        src.optString("Password"),
        index,
        DevSpecMetadata("", 0, SourceMetadata(""))
    ) {
        try {
            val obj = HttpHelper.fetchApiObject(this, MetadataManager.API_SUB_DEVSPEC)
            if (obj != null) DevSpec = DevSpecMetadata(obj, this)
            Log.i("BCSDebug", Name + " Throttle " + DevSpec.Throttle + " Notice " + DevSpec.Notice)
        } catch (ex: Exception) {
            //Discard it. Server does not support DevSpec.
        }
    }

    constructor (url: String) : this(
        "手动设定源服务器",
        "Source Server Manually Specified",
        url,
        "",
        "httpSimple",
        "未知",
        "Unknown",
        "",
        "Unknown",
        "",
        "",
        IndexMetadata(),
        DevSpecMetadata("", 0, null)
    )

    val Name: String
        get() {
            return chooseString(Name_LO, Name_EN)
        }
    val Author: String
        get() {
            return chooseString(Author_LO, Author_EN)
        }
}