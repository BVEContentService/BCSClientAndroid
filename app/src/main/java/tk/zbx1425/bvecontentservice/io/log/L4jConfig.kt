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

package tk.zbx1425.bvecontentservice.io.log

import android.os.Environment
import android.util.Log
import de.mindpipe.android.logging.log4j.LogConfigurator
import org.apache.log4j.Level
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.BuildConfig


object L4jConfig {
    private const val MAX_FILE_SIZE = 1024 * 1024 * 10L
    private const val FILE_DIR = "/bveContentService"
    private const val DEFAULT_LOG_FILE_NAME = "/log.txt"
    private const val DEFAULT_ERR_FILE_NAME = "/err.txt"
    private const val TAG = "Log4jConfigure"
    private const val PACKAGE_NAME = BuildConfig.APPLICATION_ID
    lateinit var logFile: String

    fun configure() {
        val logConfigurator = LogConfigurator()
        try {
            logFile = if (isSdcardMounted()) {
                Environment.getExternalStorageDirectory().toString() + FILE_DIR + DEFAULT_LOG_FILE_NAME
            } else {
                ApplicationContext.context.filesDir.path + DEFAULT_LOG_FILE_NAME
            }
            logConfigurator.maxBackupSize = 4
            logConfigurator.maxFileSize = MAX_FILE_SIZE
            logConfigurator.isImmediateFlush = true
            logConfigurator.filePattern = "%d\t%p/%c:\t%m%n"

            logConfigurator.rootLevel = Level.DEBUG
            logConfigurator.isUseLogCatAppender = true
            logConfigurator.fileName = logFile
            logConfigurator.configure()
            /*logConfigurator.rootLevel = Level.ERROR
            logConfigurator.isUseLogCatAppender = false
            logConfigurator.fileName = logDir + DEFAULT_ERR_FILE_NAME
            logConfigurator.configure()*/
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