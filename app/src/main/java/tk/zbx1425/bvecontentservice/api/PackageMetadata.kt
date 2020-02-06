package tk.zbx1425.bvecontentservice.api

import org.json.JSONObject
import java.io.Serializable
import java.util.*

data class PackageMetadata(
    var ID: String,
    var Version: Version,
    var File: String,
    var Author: AuthorMetadata,
    var Name_LO: String,
    var Name_EN: String,
    var Name_SA: String,
    var Origin_LO: String,
    var Origin_EN: String,
    var Origin_SA: String,
    var Homepage: String,
    var Description: String,
    var Thumbnail: String,
    var AutoOpen: Boolean,
    var ForceView: Boolean,
    var Timestamp: Date,
    var Source: SourceMetadata
) : Comparable<PackageMetadata>, Serializable {

    val searchAssistName: String = Name_LO + Name_EN + Name_SA +
            Author.Name_LO + Author.Name_EN + Author.Name_SA

    val VSID: String = this.ID + "_" + this.Version.get()

    constructor (src: JSONObject, manager: MetadataManager, source: SourceMetadata) : this(
        src.getString("ID"),
        Version(src.getString("Version")),
        src.getString("File_H2"),
        manager.getAuthor(src.tryString("Author")),
        src.tryString("Name_LO"),
        src.tryString("Name_EN"),
        src.tryString("Name_SA"),
        src.tryString("Origin_LO"),
        src.tryString("Origin_EN"),
        src.tryString("Origin_SA"),
        src.tryString("Homepage"),
        src.tryString("Description"),
        src.tryString("Thumbnail"),
        src.tryString("AutoOpen").equals("true", true),
        src.tryString("ForceView").equals("true", true),
        Date(src.getLong("TimeStamp") * 1000),
        source
    ) {
        if (this.Origin_LO != "" || this.Origin_EN != "") {
            Author = Author.copy() //Dynamic repost author
            Author.Name_LO = Origin_LO + " & " + Author.Name_LO
            Author.Name_EN = Origin_EN + " & " + Author.Name_EN
            Author.Name_SA = Origin_SA + " & " + Author.Name_SA
        }
    }

    override fun compareTo(other: PackageMetadata): Int {
        return compareValuesBy(this, other, PackageMetadata::Timestamp)
    }
}