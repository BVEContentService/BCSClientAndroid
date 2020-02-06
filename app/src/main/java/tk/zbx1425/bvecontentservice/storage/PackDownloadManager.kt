package tk.zbx1425.bvecontentservice.storage

import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.util.Log
import android.widget.Toast
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import java.io.*


object PackDownloadManager {

    const val LOGCAT_TAG = "BCSDownloadManager"
    var downloadManager: DownloadManager? = null
    var contentResolver: ContentResolver? = null
    val downloadingMap = HashMap<String, Long>()

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            Log.i(LOGCAT_TAG, "Download completed " + id)
            val dm: DownloadManager =
                downloadManager ?: throw NullPointerException("Cannot get downloadManager")
            val query = DownloadManager.Query()
            val cursor: Cursor = dm.query(query.setFilterById(id))
            if (cursor.moveToFirst()) {
                val targetVSID = downloadingMap.filterValues { it == id }.keys.elementAt(0)
                val targetFile = PackLocalManager.getLocalPackFile(targetVSID)
                Log.i(
                    LOGCAT_TAG,
                    "Received status " + cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                )
                when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val localFileUri = Uri.parse(
                            cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                        )
                        val localFile = contentResolver?.openInputStream(localFileUri)
                            ?: throw java.lang.NullPointerException("Failed get ContentResolver")
                        Log.i(LOGCAT_TAG, "Downloaded file at " + localFileUri.toString())
                        Log.i(LOGCAT_TAG, "Moving file to " + targetFile.absolutePath)
                        if (!targetFile.exists()) targetFile.createNewFile()
                        copy(localFile, targetFile)
                        Log.i(LOGCAT_TAG, "Removing cache file")
                        contentResolver!!.delete(localFileUri, null, null)
                        Log.i(LOGCAT_TAG, "Notifying PackListManager and AdapterManager")
                        PackListManager.populate()
                        Toast.makeText(
                            ApplicationContext.context, String.format(
                                ApplicationContext.context.resources.getText(R.string.info_download_finished)
                                    .toString(), targetVSID
                            ), Toast.LENGTH_SHORT
                        ).show()
                    }
                    DownloadManager.STATUS_FAILED -> {
                        Toast.makeText(
                            ApplicationContext.context, String.format(
                                ApplicationContext.context.resources.getText(R.string.info_download_failed)
                                    .toString(), targetVSID
                            ), Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Log.e(LOGCAT_TAG, "Cannot query DownloadManager Cursor")
            }
            downloadingMap.values.remove(id)
        }
    }

    fun register(context: Context) {
        downloadManager =
            context.applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        contentResolver = context.applicationContext.contentResolver
        context.applicationContext.registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        Log.i(LOGCAT_TAG, "Manager and Receiver registered")
    }

    fun getProgress(metadata: PackageMetadata): Int {
        val dm: DownloadManager = downloadManager ?: return -200
        if (downloadingMap.containsKey(metadata.VSID)) {
            val query = DownloadManager.Query()
            val cursor: Cursor = dm.query(query.setFilterById(downloadingMap[metadata.VSID]!!))
            if (cursor.moveToFirst()) {
                val bytesDownloaded: Long =
                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal: Long =
                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                cursor.close()
                Log.i(LOGCAT_TAG, "Progress " + (bytesDownloaded * 100 / bytesTotal).toInt())
                return (bytesDownloaded * 100 / bytesTotal).toInt()
            } else {
                return -200
            }
        } else {
            return -100
        }
    }

    fun startDownload(metadata: PackageMetadata): Boolean {
        Log.i(LOGCAT_TAG, "Trying download manager...")
        val dm: DownloadManager = downloadManager ?: return false
        Log.i(LOGCAT_TAG, "Got download manager")
        if (downloadingMap.containsKey(metadata.VSID)) return true
        Log.i(LOGCAT_TAG, "Not in VSID-db, starting")
        try {
            PackLocalManager.ensureHmmsimDir()
            val request =
                DownloadManager.Request(Uri.parse(metadata.File))
                    .setTitle(metadata.Name_LO) // Title of the Download Notification
                    .setDescription(metadata.File) // Description of the Download Notification
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // Visibility of the download Notification
                    //.setDestinationUri(Uri.fromFile(file)) // Uri of the destination file
                    .setAllowedOverMetered(true) // Set if download is allowed on Mobile network
                    .setAllowedOverRoaming(true) // Set if download is allowed on roaming network
            val downloadID = dm.enqueue(request)
            downloadingMap[metadata.VSID] = downloadID
            Log.i(LOGCAT_TAG, "Download started " + downloadID)
            return true
        } catch (ex: Exception) {
            Log.i(LOGCAT_TAG, ex.message)
            ex.printStackTrace()
            return false
        }
    }

    fun abortDownload(metadata: PackageMetadata): Boolean {
        val dm: DownloadManager = downloadManager ?: return false
        if (!downloadingMap.containsKey(metadata.VSID)) return false
        try {
            dm.remove(downloadingMap[metadata.VSID]!!)
            downloadingMap.remove(metadata.VSID)
            Log.i(LOGCAT_TAG, "Download aborted " + metadata.VSID)
            return true
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    @Throws(IOException::class)
    fun copy(src: InputStream, dst: File) {
        try {
            val out: OutputStream = FileOutputStream(dst)
            try { // Transfer bytes from in to out
                val buf = ByteArray(1024)
                var len: Int
                while (src.read(buf).also { len = it } > 0) {
                    out.write(buf, 0, len)
                }
            } finally {
                out.close()
            }
        } finally {
            src.close()
        }
    }
}