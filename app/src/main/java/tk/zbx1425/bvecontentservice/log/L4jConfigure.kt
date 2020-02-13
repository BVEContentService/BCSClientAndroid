package tk.zbx1425.bvecontentservice.log

import android.os.Environment
import android.util.Log
import de.mindpipe.android.logging.log4j.LogConfigurator
import org.apache.log4j.Level
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.BuildConfig


object L4jConfigure {
    private const val MAX_FILE_SIZE = 1024 * 1024 * 10L
    private const val DEFAULT_LOG_DIR = "/bveContentService"
    private const val DEFAULT_LOG_FILE_NAME = "/log.txt"
    private const val DEFAULT_ERR_FILE_NAME = "/err.txt"
    private const val TAG = "Log4jConfigure"
    private const val PACKAGE_NAME = BuildConfig.APPLICATION_ID

    fun configure() {
        val logConfigurator = LogConfigurator()
        try {
            val logDir = if (isSdcardMounted()) {
                Environment.getExternalStorageDirectory().toString() + DEFAULT_LOG_DIR
            } else {
                ApplicationContext.context.filesDir.path
            }
            logConfigurator.maxBackupSize = 4
            logConfigurator.maxFileSize = MAX_FILE_SIZE
            logConfigurator.isImmediateFlush = true
            logConfigurator.filePattern = "%d\t%p/%c:\t%m%n"

            logConfigurator.rootLevel = Level.DEBUG
            logConfigurator.isUseLogCatAppender = true
            logConfigurator.fileName = logDir + DEFAULT_LOG_FILE_NAME
            logConfigurator.configure()
            logConfigurator.rootLevel = Level.ERROR
            logConfigurator.isUseLogCatAppender = false
            logConfigurator.fileName = logDir + DEFAULT_ERR_FILE_NAME
            logConfigurator.configure()
            Log.i(TAG, "Log4j config finish")
        } catch (throwable: Throwable) {
            logConfigurator.isResetConfiguration = true
            Log.e(TAG, "Log4j config error, use default config. Error:$throwable")
        }
    }

    private fun isSdcardMounted(): Boolean {
        return Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
    }
}