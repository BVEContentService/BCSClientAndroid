package tk.zbx1425.bvecontentservice.api.model

import androidx.preference.PreferenceManager
import org.json.JSONObject
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.Version
import tk.zbx1425.bvecontentservice.chooseString
import tk.zbx1425.bvecontentservice.tryString
import java.io.Serializable
import java.util.*

data class PackageMetadata(
    var ID: String,
    var Version: Version,
    var File_REL: String,
    var FileSize: String,
    var Author: AuthorMetadata,
    var Name_LO: String,
    var Name_EN: String,
    var Name_SA: String,
    var Origin_LO: String,
    var Origin_EN: String,
    var Origin_SA: String,
    var Homepage: String,
    var Description_REL: String,
    var Thumbnail_REL: String,
    var ThumbnailLQ_REL: String,
    var AutoOpen: Boolean,
    var ForceView: Boolean,
    var Timestamp: Date,
    var Source: SourceMetadata,
    var SpiderSourceURL: String = "",
    var SpiderSourceUsername: String = "",
    var UpdateAvailable: Boolean = false
) : Comparable<PackageMetadata>, Serializable {

    val searchAssistName: String = (Name_LO + Name_EN + Name_SA +
            Author.Name_LO + Author.Name_EN + Author.Name_SA).toLowerCase(Locale.US)

    val VSID: String = this.ID + "_" + this.Version.get()

    constructor (
        src: JSONObject, manager: MetadataManager, source: SourceMetadata
        , bySpider: Boolean = false
    ) : this(
        src.getString("ID"),
        Version(src.getString("Version")),
        src.tryString("File_H2"),
        src.tryString("FileSize_H2"),
        MetadataManager.getAuthor(
            src.tryString(
                "Author"
            )
        ) ?: AuthorMetadata(source),
        src.tryString("Name_LO"),
        src.tryString("Name_EN"),
        src.tryString("Name_SA"),
        src.tryString("Origin_LO"),
        src.tryString("Origin_EN"),
        src.tryString("Origin_SA"),
        src.tryString("Homepage"),
        src.tryString("Description"),
        src.tryString("Thumbnail"),
        src.tryString("ThumbnailLQ"),
        src.tryString("AutoOpen").equals("1", true),
        src.tryString("ForceView").equals("1", true),
        Date(src.getLong("TimeStamp") * 1000),
        source,
        src.tryString("SourceURL"),
        src.tryString("SourceUsername")
    ) {
        if (bySpider) {
            Source = MetadataManager.sourceServers.find {
                it.APIURL == SpiderSourceURL &&
                        it.Username == SpiderSourceUsername
            } ?: throw NullPointerException("Bad Spider Source!")
        }
    }

    override fun compareTo(other: PackageMetadata): Int {
        return compareValuesBy(this, other, PackageMetadata::Timestamp)
    }

    val Name: String
        get() {
            return chooseString(Name_LO, Name_EN)
        }
    val Origin: String
        get() {
            return chooseString(Origin_LO, Origin_EN)
        }

    private fun processRelUrl(url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        } else if (PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context).getBoolean(
                "reverseProxy", true
            ) && Source.APIRProxy != ""
        ) {
            return Source.APIRProxy + url
        } else {
            return Source.APIURL + url
        }
    }

    val File: String
        get() {
            return processRelUrl(File_REL)
        }
    val Thumbnail: String
        get() {
            return processRelUrl(Thumbnail_REL)
        }
    val ThumbnailLQ: String
        get() {
            return processRelUrl(ThumbnailLQ_REL)
        }
    val Description: String
        get() {
            return if (Description_REL.trim().endsWith(".html")
                || Description_REL.trim().endsWith(".txt")
            ) {
                processRelUrl(Description_REL)
            } else {
                Description_REL
            }
        }
}