package tk.zbx1425.bvecontentservice.api

import org.json.JSONObject
import tk.zbx1425.bvecontentservice.chooseString
import tk.zbx1425.bvecontentservice.tryString
import java.io.Serializable

data class IndexMetadata(
    var Name_LO: String,
    var Name_EN: String,
    var APIURL: String,
    var Maintainer_LO: String,
    var Maintainer_EN: String,
    var Homepage: String,
    var Contact: String
) : Serializable {
    constructor (src: JSONObject, APIURL: String) : this(
        src.getString("Name_LO"),
        src.getString("Name_EN"),
        APIURL,
        src.getString("Maintainer_LO"),
        src.getString("Maintainer_EN"),
        src.tryString("Homepage"),
        src.getString("Contact")
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

    val Name: String
        get() {
            return chooseString(Name_LO, Name_EN)
        }
    val Maintainer: String
        get() {
            return chooseString(Maintainer_LO, Maintainer_EN)
        }
}