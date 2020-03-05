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

package tk.zbx1425.bvecontentservice

import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceManager
import tk.zbx1425.bvecontentservice.api.ManagerConfig
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata
import java.util.*

fun View.replaceView(newView: View) {
    val parent: ViewGroup = this.parent as ViewGroup
    val index = parent.indexOfChild(this)
    newView.layoutParams = this.layoutParams
    newView.setPadding(this.paddingLeft, this.paddingRight, this.paddingTop, this.paddingBottom)
    parent.removeViewAt(index)
    parent.addView(newView, index)
}

fun String.replace(replacements: Map<String, String>): String {
    var result = this
    replacements.forEach { result = result.replace(it.key, it.value) }
    return result
}

fun String.nullify(): String? {
    return if (this == "") null; else this
}

fun chooseString(lo: String, en: String): String {
    return if (getPreference("englishName", false)
    ) en; else lo
}

fun processRelUrl(Source: SourceMetadata, url: String): String {
    return if (url == "") {
        ""
    } else if (url.toLowerCase(Locale.US).startsWith("http://")
        || url.toLowerCase(Locale.US).startsWith("https://")
    ) {
        url
    } else if (ManagerConfig.reverseProxy && Source.APIRProxy != "") {
        Source.APIRProxy + url
    } else {
        Source.APIURL + url
    }
}

fun getPreference(key: String, defValue: String): String {
    return PreferenceManager.getDefaultSharedPreferences(
        ApplicationContext.context
    ).getString(key, defValue) ?: ""
}

fun getPreference(key: String, defValue: Int): Int {
    return PreferenceManager.getDefaultSharedPreferences(
        ApplicationContext.context
    ).getInt(key, defValue)
}

fun getPreference(key: String, defValue: Boolean): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(
        ApplicationContext.context
    ).getBoolean(key, defValue)
}