package tk.zbx1425.bvecontentservice

import android.app.Application


class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContext.initialize(this)
    }

}