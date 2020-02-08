package tk.zbx1425.bvecontentservice

import androidx.preference.PreferenceManager
import org.json.JSONObject

fun JSONObject.tryString(key: String?): String {
    return if (this.has(key)) this.getString(key) else ""
}

fun chooseString(lo: String, en: String): String {
    return if (PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context)
            .getBoolean("englishName", false)
    ) en; else lo
}