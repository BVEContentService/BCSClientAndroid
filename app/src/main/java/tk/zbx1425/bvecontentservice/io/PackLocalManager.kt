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

package tk.zbx1425.bvecontentservice.io

import android.os.Environment
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.log.Log
import java.io.File
import java.io.IOException
import java.util.*

object PackLocalManager {
    val BCS_DELIMITER = 0x202C.toChar()
    val BCS_MAGIC_CHAR = arrayOf(
        0x200B.toChar(), 0x200C.toChar(), 0x2060.toChar(), 0xFEFF.toChar(),
        0x202A.toChar(), 0x202B.toChar(), 0x200D.toChar(), 0x202D.toChar()
    )
    val BCS_SUFFIX = "." + encodeInvisibleString("bcs")
    val hmmDir = File(Environment.getExternalStorageDirectory(), "Hmmsim")
    val appDir = File(Environment.getExternalStorageDirectory(), "bveContentService")

    fun encodeInvisibleString(src: String): String {
        val builder = StringBuilder()
        for (c in src.toCharArray()) {
            var str = String.format("%02d", Integer.parseInt(Integer.toString(c.toInt(), 8)))
            for (i in BCS_MAGIC_CHAR.indices) {
                str = str.replace((0x30 + i).toChar(), BCS_MAGIC_CHAR[i])
            }
            builder.append(str)
        }
        return builder.toString()
    }

    fun decodeInvisibleString(src: String): String {
        val chars = CharArray(src.length / 2)
        var i = 0
        while (i < src.length) {
            var str = src.substring(i, i + 2)
            for (j in BCS_MAGIC_CHAR.indices) {
                str = str.replace(BCS_MAGIC_CHAR[j], (0x30 + j).toChar())
            }
            val nb = Integer.parseInt(str, 8)
            chars[i / 2] = nb.toChar()
            i += 2
        }
        return String(chars)
    }

    fun getLocalState(metadata: PackageMetadata): Int {
        ensureHmmsimDir()
        val packFile = getLocalPackFile(metadata)
        if (packFile.exists()) {
            return 200
        } else if (PackDownloadManager.downloadingMap.containsKey(metadata.VSID)) {
            return PackDownloadManager.getProgress(metadata)
        } else {
            return -100
        }
    }

    fun getLocalPackFile(metadata: PackageMetadata): File {
        return getLocalPackFile(metadata.VSID)
    }

    fun convertVSID(VSID: String): String {
        val tokens = VSID.reversed().split('_', limit = 2)
        return encodeInvisibleString(tokens[0].reversed()) + BCS_DELIMITER + tokens[1].reversed()
    }

    fun getLocalPackFile(VSID: String): File {
        return File(hmmDir, BCS_SUFFIX + BCS_DELIMITER + convertVSID(VSID) + BCS_DELIMITER + ".zip")
    }

    fun getLocalTempFile(VSID: String): File {
        return File(hmmDir, BCS_SUFFIX + BCS_DELIMITER + convertVSID(VSID) + BCS_DELIMITER + ".tmp")
    }

    fun getUpdateTempFile(): File {
        val tempFile = File(appDir, "update.apk")
        //if (tempFile.exists()) Log.i("DEBUG", tempFile.delete().toString())
        return tempFile
    }

    fun flushCache() {
        val allCache = hmmDir.listFiles { file: File ->
            file.isFile && file.name.toLowerCase(Locale.US).endsWith(".tmp")
        } ?: arrayOf()
        for (cache in allCache) {
            cache.delete()
        }
    }

    fun removeLocalPack(RelPath: String) {
        val file = File(hmmDir, RelPath)
        file.delete()
    }

    fun removeLocalPacks(ID: String) {
        for (file in getLocalPacks()) {
            val parts = file.nameWithoutExtension.split(
                BCS_DELIMITER
            )
            if (parts.count() > 3 && parts[2] == ID) {
                file.delete()
            }
        }
    }

    fun getLocalPacks(): Array<File> {
        return hmmDir.listFiles { file: File ->
            file.isFile &&
                    file.name.toLowerCase(Locale.US).endsWith(".zip") &&
                    file.name.toLowerCase(Locale.US).startsWith(BCS_SUFFIX)
        } ?: arrayOf()
    }

    fun trimEncryptedName(src: String): String {
        return src.filterNot { it in BCS_MAGIC_CHAR || it == '.' || it == BCS_DELIMITER }
    }

    fun ensureHmmsimDir() {
        if (!hmmDir.exists()) {
            Log.i(PackDownloadManager.LOGCAT_TAG, "Hmmdir not there. Creating...")
            if (!hmmDir.mkdirs()) {
                throw IOException("Unable create Hmmsim folder")
            }
        }
        val noMediaHint = File(hmmDir, ".nomedia")
        if (!noMediaHint.exists()) noMediaHint.createNewFile()
        deleteUnqualifiedFile()
    }

    fun ensureAppDir() {
        if (!appDir.exists()) {
            Log.i(PackDownloadManager.LOGCAT_TAG, "AppDir not there. Creating...")
            if (!appDir.mkdirs()) {
                throw IOException("Unable create AppDir folder")
            }
        }
    }

    fun deleteUnqualifiedFile() {
        //Evil little trick.
        hmmDir.listFiles { file: File ->
            file.isFile &&
                    file.name.toLowerCase(Locale.US).startsWith(".") &&
                    !file.name.toLowerCase(Locale.US).startsWith(BCS_SUFFIX)
        }?.forEach { it.delete() }
    }
}