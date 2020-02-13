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