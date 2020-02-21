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
import tk.zbx1425.bvecontentservice.api.Version
import tk.zbx1425.bvecontentservice.chooseString
import tk.zbx1425.bvecontentservice.processRelUrl

import java.io.Serializable

data class UpdateMetadata(
    var Version: Version,
    var File_REL: String,
    var CrashReport_REL: String,
    var ReportMethod: String,
    var Description_LO: String,
    var Description_EN: String,
    var Homepage: String,
    var Force: Boolean,
    val Source: SourceMetadata
) : Serializable {
    constructor (src: JSONObject, Source: SourceMetadata) : this(
        Version(src.getString("Version")),
        src.getString("File"),
        src.optString("CrashReport"),
        src.optString("ReportMethod"),
        src.getString("Description_LO"),
        src.getString("Description_EN"),
        src.optString("Homepage"),
        src.optBoolean("Force", false),
        Source
    )

    fun chooseNewer(opponent: UpdateMetadata?): UpdateMetadata {
        return if (this.Version > opponent?.Version ?: return this) this; else opponent
    }

    val Description: String
        get() {
            return chooseString(Description_LO, Description_EN)
        }

    val File: String
        get() {
            return processRelUrl(Source, File_REL)
        }

    val CrashReport: String
        get() {
            return processRelUrl(Source, CrashReport_REL)
        }
}