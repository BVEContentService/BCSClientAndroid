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
import tk.zbx1425.bvecontentservice.chooseString
import tk.zbx1425.bvecontentservice.processRelUrl

import java.io.Serializable
import java.util.*


data class AuthorMetadata(
    var ID: String,
    var Name_LO: String,
    var Name_EN: String,
    var Name_SA: String,
    var Homepage: String,
    var Description_REL: String,
    var Source: SourceMetadata
) : Serializable {
    constructor (src: JSONObject, source: SourceMetadata) : this(
        src.getString("ID"),
        src.getString("Name_LO"),
        src.getString("Name_EN"),
        src.optString("Name_SA"),
        src.optString("Homepage"),
        src.optString("Description"),
        source
    )

    constructor(source: SourceMetadata) : this(
        "Unknown",
        "Unknown",
        "Unknown",
        "Unknown",
        "",
        "",
        source
    )

    val Name: String
        get() {
            return chooseString(Name_LO, Name_EN)
        }
    val Description: String
        get() {
            return if (Description_REL.startsWith("/")
                || Description_REL.toLowerCase(Locale.US).startsWith("http://")
                || Description_REL.toLowerCase(Locale.US).startsWith("https://")
            ) {
                processRelUrl(Source, Description_REL)
            } else {
                Description_REL
            }
        }
}