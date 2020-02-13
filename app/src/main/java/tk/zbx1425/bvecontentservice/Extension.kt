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
import org.json.JSONObject

fun JSONObject.tryString(key: String?): String {
    return if (this.has(key)) this.getString(key) else ""
}

fun View.replaceView(newView: View) {
    val parent: ViewGroup = this.parent as ViewGroup
    val index = parent.indexOfChild(this)
    newView.layoutParams = this.layoutParams
    newView.setPadding(this.paddingLeft, this.paddingRight, this.paddingTop, this.paddingBottom)
    parent.removeViewAt(index)
    parent.addView(newView, index)
}

fun chooseString(lo: String, en: String): String {
    return if (PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context)
            .getBoolean("englishName", false)
    ) en; else lo
}