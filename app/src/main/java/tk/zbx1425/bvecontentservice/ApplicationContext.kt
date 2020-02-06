package tk.zbx1425.bvecontentservice

import android.content.Context

object ApplicationContext {
    @Volatile
    lateinit var context: Context

    fun initialize(context: Context) {
        this.context = context
    }
}