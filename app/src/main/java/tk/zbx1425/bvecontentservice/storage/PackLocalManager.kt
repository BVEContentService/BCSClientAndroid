package tk.zbx1425.bvecontentservice.storage

import android.os.Environment
import android.util.Log
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import java.io.File
import java.io.IOException
import java.util.*

object PackLocalManager {
    val BCS_SUFFIX = "." + encodeInvisibleString("bcs")
    val BCS_DELIMITER = 0x200D.toChar()
    val hmmDir = File(Environment.getExternalStorageDirectory(), "Hmmsim")

    fun encodeInvisibleString(src: String): String {
        val builder = StringBuilder()
        for (c in src.toCharArray()) {
            val toString = Integer.toString(c.toInt(), 2)
            builder.append(
                String.format("%08d", Integer.parseInt(toString))
                    .replace('0', 0x200B.toChar()).replace('1', 0x200C.toChar())
            )
        }
        return builder.toString()
    }

    fun decodeInvisibleString(src: String): String {
        val chars = CharArray(src.length / 8)
        var i = 0
        while (i < src.length) {
            val str = src.substring(i, i + 8)
                .replace(0x200B.toChar(), '0').replace(0x200C.toChar(), '1')
            val nb = Integer.parseInt(str, 2)
            chars[i / 8] = nb.toChar()
            i += 8
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

    fun flushCache() {
        val allCache = hmmDir.listFiles { file: File ->
            file.isFile && file.name.toLowerCase(Locale.US).endsWith(".tmp")
        } ?: arrayOf()
        for (cache in allCache) {
            cache.delete()
        }
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

    fun deleteUnqualifiedFile() {
        //Evil little trick.
        hmmDir.listFiles { file: File ->
            file.isFile &&
                    file.name.toLowerCase(Locale.US).startsWith(".") &&
                    !file.name.toLowerCase(Locale.US).startsWith(BCS_SUFFIX)
        }?.forEach { it.delete() }
    }
}