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

import Identification
import org.json.JSONObject
import tk.zbx1425.bvecontentservice.processRelUrl
import java.io.Serializable
import java.util.*

class DevSpecMetadata : Serializable {
    val Notice_REL: String
    val Throttle: Int
    val Source: SourceMetadata?

    constructor(Notice_REL: String, Throttle: Int, Source: SourceMetadata?) {
        this.Notice_REL = Notice_REL
        this.Throttle = Throttle
        this.Source = Source
    }

    constructor(src: JSONObject, Source: SourceMetadata) {
        val throttleList = src.getJSONObject("Throttle")
        Throttle = if (throttleList.has(Identification.deviceID)) {
            throttleList.getInt(Identification.deviceID)
        } else {
            throttleList.optInt("*", 0)
        }
        Notice_REL = if (src.optBoolean("NoticeOnlyForThrottled", false)) {
            if (Throttle != 0) {
                src.optString("Notice", "")
            } else {
                ""
            }
        } else {
            src.optString("Notice", "")
        }
        this.Source = Source
    }

    val Notice: String
        get() {
            return if (Notice_REL.startsWith("/")
                || Notice_REL.toLowerCase(Locale.US).startsWith("http://")
                || Notice_REL.toLowerCase(Locale.US).startsWith("https://")
            ) {
                if (Source != null) {
                    processRelUrl(Source, Notice_REL)
                } else {
                    Notice_REL
                }
            } else {
                Notice_REL
            }
        }
}