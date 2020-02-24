package tk.zbx1425.bvecontentservice.api

import tk.zbx1425.bvecontentservice.log.Log

class SimpleStringMap : HashMap<String, String>() {

    fun putAll(fromData: String) {
        val lines = fromData.lines()
        for (i in 0 until lines.count() / 2) {
            this[lines[2 * i]] = lines[2 * i + 1]
        }
        Log.i("BCSDebug", this.toString())
    }
}