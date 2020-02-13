package tk.zbx1425.bvecontentservice.api.model

import org.json.JSONObject
import tk.zbx1425.bvecontentservice.chooseString
import tk.zbx1425.bvecontentservice.tryString
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
    val Index: IndexMetadata
) : Serializable {
    constructor (src: JSONObject, index: IndexMetadata) : this(
        src.getString("Name_LO"),
        src.getString("Name_EN"),
        src.getString("APIURL"),
        src.tryString("APIRProxy"),
        src.getString("APIType"),
        src.getString("Author_LO"),
        src.getString("Author_EN"),
        src.tryString("Homepage"),
        src.getString("Contact"),
        src.tryString("Username"),
        src.tryString("Password"),
        index
    )

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
        IndexMetadata()
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