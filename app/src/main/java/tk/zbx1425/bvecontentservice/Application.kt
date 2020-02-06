package tk.zbx1425.bvecontentservice

import android.app.Application
import tk.zbx1425.bvecontentservice.storage.PackDownloadManager


class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContext.initialize(this)
        PackDownloadManager.register(this)
    }
}