package tk.zbx1425.bvecontentservice.api

import org.json.JSONObject
import tk.zbx1425.bvecontentservice.chooseString
import tk.zbx1425.bvecontentservice.tryString
import java.io.Serializable

open class OnymousMetadata(
    var Name_LO: String = "N/A",
    var Name_EN: String = "N/A",
    var Author_LO: String = "N/A",
    var Author_EN: String = "N/A",
    var Homepage: String = "",
    var Contact: String = "N/A"
) : Serializable {
    constructor (src: JSONObject?) : this(
        src?.getString("Name_LO") ?: "N/A",
        src?.getString("Name_EN") ?: "N/A",
        src?.getString("Author_LO") ?: "N/A",
        src?.getString("Author_EN") ?: "N/A",
        src?.tryString("Homepage") ?: "",
        src?.getString("Contact") ?: "N/A"
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