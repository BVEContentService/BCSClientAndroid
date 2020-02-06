package tk.zbx1425.bvecontentservice.storage

import android.os.Environment
import android.util.Log
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import java.io.File
import java.io.IOException

object PackLocalManager {
    val hmmDir = File(Environment.getExternalStorageDirectory(), "Hmmsim")

    fun getLocalState(metadata: PackageMetadata): Int {
        ensureHmmsimDir()
        val packFile = File(hmmDir, metadata.VSID + ".bcs.zip")
        if (packFile.exists()) {
            return 200
        } else if (PackDownloadManager.downloadingMap.containsKey(metadata.VSID)) {
            return PackDownloadManager.getProgress(metadata)
        } else {
            return -100
        }
    }

    fun getLocalPackFile(VSID: String): File {
        return File(hmmDir, VSID + ".bcs.zip")
    }

    fun removeLocalPacks(ID: String) {
        for (file in getLocalPacks()) {
            val parts = PackListManager.stripExtension(file.nameWithoutExtension).split("_")
            if (parts.count() > 1 && parts[0] == ID) {
                file.delete()
            }
        }
    }

    fun getLocalPacks(): Array<File> {
        return hmmDir.listFiles { file: File ->
            file.isFile &&
                    file.name.toLowerCase().endsWith(".bcs.zip")
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