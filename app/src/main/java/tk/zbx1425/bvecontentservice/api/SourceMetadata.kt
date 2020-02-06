package tk.zbx1425.bvecontentservice.api

import org.json.JSONObject
import java.io.Serializable

data class SourceMetadata(
    var Name_LO: String,
    var Name_EN: String,
    var APIURL: String,
    var Maintainer: String,
    var Homepage: String,
    var Contact: String,
    var Index: IndexMetadata
) : Serializable {
    constructor (src: JSONObject, index: IndexMetadata) : this(
        src.getString("Name_LO"),
        src.getString("Name_EN"),
        src.getString("APIURL"),
        src.getString("Maintainer"),
        src.tryString("Homepage"),
        src.getString("Contact"),
        index
    )
}