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
import tk.zbx1425.bvecontentservice.tryString
import java.io.Serializable

data class IndexMetadata(
    var Name_LO: String,
    var Name_EN: String,
    var APIURL: String,
    var Author_LO: String,
    var Author_EN: String,
    var Homepage: String,
    var Contact: String,
    var Protocol: String = ""
) : Serializable {
    constructor (src: JSONObject, APIURL: String) : this(
        src.getString("Name_LO"),
        src.getString("Name_EN"),
        APIURL,
        src.getString("Author_LO"),
        src.getString("Author_EN"),
        src.tryString("Homepage"),
        src.getString("Contact"),
        src.tryString("Protocol")
    )

    constructor() : this(
        "未使用索引服务器",
        "Index Server Not Used",
        "None",
        "未使用",
        "None",
        "",
        "None"
    )

    constructor(APIURL: String) : this(
        "手动设定",
        "Manually Specified",
        APIURL,
        "未知",
        "Unknown",
        "",
        "Unknown"
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