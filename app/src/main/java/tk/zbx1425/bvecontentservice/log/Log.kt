package tk.zbx1425.bvecontentservice.log

import android.text.TextUtils
import org.apache.log4j.Logger
import tk.zbx1425.bvecontentservice.BuildConfig

object Log {
    var isConfigured: Boolean = false
    var SWITCH_LOG: Boolean = BuildConfig.DEBUG

    fun d(tag: String, message: String) {
        if (SWITCH_LOG) {
            val LOGGER = getLogger(tag)
            LOGGER.debug(message)
        }
    }

    fun d(tag: String, message: String, exception: Throwable) {
        if (SWITCH_LOG) {
            val LOGGER = getLogger(tag)
            LOGGER.debug(message, exception)
        }
    }

    fun i(tag: String, message: String) {
        if (SWITCH_LOG) {
            val LOGGER = getLogger(tag)
            LOGGER.info(message)
        }
    }

    fun i(tag: String, message: String, exception: Throwable) {
        if (SWITCH_LOG) {
            val LOGGER = getLogger(tag)
            LOGGER.info(message, exception)
        }
    }

    fun w(tag: String, message: String) {
        if (SWITCH_LOG) {
            val LOGGER = getLogger(tag)
            LOGGER.warn(message)
        }
    }

    fun w(tag: String, message: String, exception: Throwable) {
        if (SWITCH_LOG) {
            val LOGGER = getLogger(tag)
            LOGGER.warn(message, exception)
        }
    }

    fun e(tag: String, message: String) {
        if (SWITCH_LOG) {
            val LOGGER = getLogger(tag)
            LOGGER.error(message)
        }
    }

    fun e(tag: String, message: String, exception: Throwable) {
        if (SWITCH_LOG) {
            val LOGGER = getLogger(tag)
            LOGGER.error(message, exception)
        }
    }

    private fun getLogger(tag: String): Logger {
        if (!isConfigured) {
            L4jConfigure.configure()
            isConfigured = true
        }
        val logger: Logger
        if (TextUtils.isEmpty(tag)) {
            logger = Logger.getRootLogger()
        } else {
            logger = Logger.getLogger(tag)
        }
        return logger
    }
}