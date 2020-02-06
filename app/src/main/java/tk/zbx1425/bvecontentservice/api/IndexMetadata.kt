package tk.zbx1425.bvecontentservice.api

import org.json.JSONObject
import java.io.Serializable

data class IndexMetadata(
    var Name_LO: String,
    var Name_EN: String,
    var APIURL: String,
    var Maintainer: String,
    var Homepage: String,
    var Contact: String
) : Serializable {
    constructor (src: JSONObject, APIURL: String) : this(
        src.getString("Name_LO"),
        src.getString("Name_EN"),
        APIURL,
        src.getString("Maintainer"),
        src.tryString("Homepage"),
        src.getString("Contact")
    )
}