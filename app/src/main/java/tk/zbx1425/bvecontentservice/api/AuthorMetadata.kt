package tk.zbx1425.bvecontentservice.api

import org.json.JSONObject
import tk.zbx1425.bvecontentservice.chooseString
import tk.zbx1425.bvecontentservice.tryString
import java.io.Serializable


data class AuthorMetadata(
    var ID: String,
    var Name_LO: String,
    var Name_EN: String,
    var Name_SA: String,
    var Homepage: String,
    var Description: String
) : Serializable {
    constructor (src: JSONObject) : this(
        src.getString("ID"),
        src.getString("Name_LO"),
        src.getString("Name_EN"),
        src.tryString("Name_SA"),
        src.tryString("Homepage"),
        src.tryString("Description")
    )

    val Name: String
        get() {
            return chooseString(Name_LO, Name_EN)
        }
}