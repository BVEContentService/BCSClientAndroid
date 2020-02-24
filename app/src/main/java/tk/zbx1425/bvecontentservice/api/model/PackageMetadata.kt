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
import tk.zbx1425.bvecontentservice.api.ManagerConfig
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.Version
import tk.zbx1425.bvecontentservice.chooseString
import tk.zbx1425.bvecontentservice.io.PackLocalManager
import tk.zbx1425.bvecontentservice.processRelUrl

import java.io.Serializable
import java.util.*

data class PackageMetadata(
    var ID: String = "",
    var Version: Version = Version("0.0"),
    var File_REL: String = "",
    var Referer: String = "",
    var FileSize: String = "",
    var Author: AuthorMetadata = AuthorMetadata(SourceMetadata("")),
    var Name_LO: String = "",
    var Name_EN: String = "",
    var Name_SA: String = "",
    var Origin_LO: String = "",
    var Origin_EN: String = "",
    var Origin_SA: String = "",
    var Homepage: String = "",
    var Description_REL: String = "",
    var Thumbnail_REL: String = "",
    var ThumbnailLQ_REL: String = "",
    var NoFile: Boolean = false,
    var AutoOpen: Boolean = false,
    var ForceView: Boolean = false,
    var Timestamp: Date = Date(),
    var Source: SourceMetadata = SourceMetadata(""),
    var UpdateAvailable: Boolean = false,
    var DummyPack: Boolean = false
) : Comparable<PackageMetadata>, Serializable {

    val searchAssistName: String = (Name_LO + Name_EN + Name_SA +
            Author.Name_LO + Author.Name_EN + Author.Name_SA).toLowerCase(Locale.US)

    val VSID: String = this.ID + "_" + this.Version.get()

    constructor (src: JSONObject, source: SourceMetadata) : this(
        src.getString("ID"),
        Version(src.getString("Version")),
        src.optString("File_" + ManagerConfig.simulator),
        src.optString("Referer_" + ManagerConfig.simulator),
        src.optString("FileSize_" + ManagerConfig.simulator),
        MetadataManager.getAuthor(src.optString("Author")) ?: AuthorMetadata(source),
        src.optString("Name_LO"),
        src.optString("Name_EN"),
        src.optString("Name_SA"),
        src.optString("Origin_LO"),
        src.optString("Origin_EN"),
        src.optString("Origin_SA"),
        src.optString("Homepage"),
        src.optString("Description"),
        src.optString("Thumbnail"),
        src.optString("ThumbnailLQ"),
        src.optBoolean("NoFile", false),
        src.optBoolean("AutoOpen", false),
        src.optBoolean("ForceView", false),
        Date(src.getLong("TimeStamp") * 1000),
        source
    )

    constructor(localName: String) : this(
        File_REL = localName, Name_LO = PackLocalManager.trimEncryptedName(localName)
        , Name_EN = PackLocalManager.trimEncryptedName(localName), DummyPack = true
    )

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

    val File: String
        get() {
            return processRelUrl(Source, File_REL)
        }
    val Thumbnail: String
        get() {
            return processRelUrl(Source, Thumbnail_REL)
        }
    val ThumbnailLQ: String
        get() {
            return processRelUrl(Source, ThumbnailLQ_REL)
        }
    val Description: String
        get() {
            return if (Description_REL.startsWith("/")
                || Description_REL.toLowerCase(Locale.US).startsWith("http://")
                || Description_REL.toLowerCase(Locale.US).startsWith("https://")
            ) {
                processRelUrl(Source, Description_REL)
            } else {
                Description_REL
            }
        }
}