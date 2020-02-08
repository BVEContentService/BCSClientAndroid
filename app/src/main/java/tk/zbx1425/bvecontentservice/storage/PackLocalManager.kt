package tk.zbx1425.bvecontentservice.storage

import android.os.Environment
import android.util.Log
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import java.io.File
import java.io.IOException
import java.util.*

object PackLocalManager {
    val hmmDir = File(Environment.getExternalStorageDirectory(), "Hmmsim")

    fun getLocalState(metadata: PackageMetadata): Int {
        ensureHmmsimDir()
        val packFile = File(hmmDir, ".bcs." + metadata.VSID + ".zip")
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

    fun getLocalPackFile(VSID: String): File {
        return File(hmmDir, ".bcs." + VSID + ".zip")
    }

    fun getLocalTempFile(VSID: String): File {
        return File(hmmDir, ".bcs." + VSID + ".tmp")
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
            val parts = PackListManager.stripExtension(file.nameWithoutExtension).split("_")
            if (parts.count() > 1 && parts[0].replace(".bcs.", "") == ID) {
                file.delete()
            }
        }
    }

    fun getLocalPacks(): Array<File> {
        return hmmDir.listFiles { file: File ->
            file.isFile &&
                    file.name.toLowerCase(Locale.US).endsWith(".zip") &&
                    file.name.toLowerCase(Locale.US).startsWith(".bcs.")
        } ?: arrayOf()
    }

    fun ensureHmmsimDir() {
        if (!hmmDir.exists()) {
            Log.i(PackDownloadManager.LOGCAT_TAG, "Hmmdir not there. Creating...")
            if (!hmmDir.mkdirs()) {
                throw IOException("Unable create Hmmsim folder")
            }
        }
    }
}