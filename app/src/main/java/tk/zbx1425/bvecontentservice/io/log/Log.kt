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

import android.text.TextUtils
import org.apache.log4j.Logger

object Log {
    var isConfigured: Boolean = false
    var SWITCH_LOG: Boolean = true

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
            L4jConfig.configure()
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