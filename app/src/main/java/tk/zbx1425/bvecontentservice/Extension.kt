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