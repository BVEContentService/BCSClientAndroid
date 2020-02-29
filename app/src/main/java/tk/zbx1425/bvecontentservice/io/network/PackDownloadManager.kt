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

package tk.zbx1425.bvecontentservice.io.network

import Identification
import android.app.DownloadManager
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.Credentials
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.io.PackListManager
import tk.zbx1425.bvecontentservice.io.PackLocalManager
import tk.zbx1425.bvecontentservice.io.log.Log
import java.io.*
import kotlin.collections.HashMap
import kotlin.collections.elementAt
import kotlin.collections.filterValues
import kotlin.collections.set
import kotlin.system.exitProcess


object PackDownloadManager {

    const val LOGCAT_TAG = "BCSDownloadManager"
    const val CALLBACK_INTERVAL = 1000L
    val MAGIC_UPDATE = PackageMetadata("__SELF_UPDATE")
    val downloadingMap = HashMap<String, Long>()
    val handlerMap = HashMap<String, Pair<((succeed: Boolean) -> Unit)?, ((bytesDownloaded: Long, bytesTotal: Long) -> Unit)?>>()
    val handler = Handler()
    var downloadManager: DownloadManager? = null
    var contentResolver: ContentResolver? = null

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
                        /*val localFile = contentResolver?.openInputStream(localFileUri)
                            ?: throw java.lang.NullPointerException("Failed get ContentResolver")*/
                        val tempFile = File(
                            ApplicationContext.context.getExternalFilesDir("downloadCache"),
                            PackLocalManager.getLocalTempFile(targetVSID).name
                        )
                        Log.i(LOGCAT_TAG, "Downloaded file at " + localFileUri.toString())
                        Log.i(LOGCAT_TAG, "Moving file to " + targetFile.absolutePath)
                        /*if (!targetFile.exists()) targetFile.createNewFile()
                        copy(localFile, targetFile)*/
                        tempFile.renameTo(PackLocalManager.getLocalPackFile(targetVSID))
                        //Log.i(LOGCAT_TAG, "Removing cache file")
                        //contentResolver!!.delete(localFileUri, null, null)
                        Log.i(LOGCAT_TAG, "Notifying PackListManager")
                        PackListManager.populate()
                        Toast.makeText(
                            ApplicationContext.context, String.format(
                                ApplicationContext.context.resources.getText(R.string.info_download_finished).toString(),
                                targetVSID, ""
                            ), Toast.LENGTH_SHORT
                        ).show()
                        downloadingMap.values.remove(id)
                        handlerMap[targetVSID]?.first?.invoke(true)
                    }
                    DownloadManager.STATUS_FAILED -> {
                        Toast.makeText(
                            ApplicationContext.context, String.format(
                                ApplicationContext.context.resources.getText(R.string.info_download_failed).toString(),
                                targetVSID, cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))
                            ), Toast.LENGTH_SHORT
                        ).show()
                        Log.e(LOGCAT_TAG, "Download failed " + id + " " + cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)))
                        downloadingMap.values.remove(id)
                        handlerMap[targetVSID]?.first?.invoke(false)
                    }
                }
            } else {
                Log.e(LOGCAT_TAG, "Cannot query DownloadManager Cursor")
            }
        }
    }

    fun register(context: Context) {
        downloadManager = context.applicationContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        contentResolver = context.applicationContext.contentResolver
        context.applicationContext.registerReceiver(
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
        Log.i(LOGCAT_TAG, "Manager and Receiver registered")
    }

    fun setHandler(
        metadata: PackageMetadata,
        finishHandler: ((succeed: Boolean) -> Unit)?, progressHandler: ((bytesDownloaded: Long, bytesTotal: Long) -> Unit)?
    ) {
        handlerMap[metadata.VSID] = Pair(finishHandler, progressHandler)
    }

    fun startDownload(
        metadata: PackageMetadata, url: String,
        referer: String? = null, cookie: String? = null
    ): Boolean {
        Log.i(LOGCAT_TAG, "Trying download manager...")
        val dm: DownloadManager = downloadManager ?: return false
        Log.i(LOGCAT_TAG, "Got download manager")
        if (downloadingMap.containsKey(metadata.VSID)) return true
        Log.i(LOGCAT_TAG, "Not in VSID-db, starting")
        try {
            PackLocalManager.ensureHmmsimDir()
            Log.i(LOGCAT_TAG, url)
            val request =
                DownloadManager.Request(Uri.parse(url))
                    /*.setTitle(metadata.Name) // Title of the Download Notification
                    .setDescription(url) // Description of the Download Notification*/
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE) // Visibility of the download Notification
                    .setDestinationInExternalFilesDir(
                        ApplicationContext.context,
                        "downloadCache", PackLocalManager.getLocalTempFile(metadata.VSID).name
                    )
            //.setDestinationUri(Uri.fromFile(file)) // Uri of the destination file
            /*.setAllowedOverMetered(true) // Set if download is allowed on Mobile network
            .setAllowedOverRoaming(true) // Set if download is allowed on roaming network*/
            request.setMimeType("application/octet-stream")
            request.addRequestHeader("User-Agent", HttpHelper.deviceUA)
            request.addRequestHeader("Referer", referer ?: HttpHelper.REFERER)
            request.addRequestHeader("cookie", cookie)
            request.addRequestHeader("X-BCS-UUID", Identification.deviceID)
            request.addRequestHeader("X-BCS-CHECKSUM", Identification.getChecksum(metadata))
            request.addRequestHeader("X-BCS-Throttle", metadata.Source.DevSpec.Throttle.toString())
            when (metadata.Source.APIType) {
                "httpBasicAuth" -> {
                    val credential: String =
                        Credentials.basic(metadata.Source.Username, metadata.Source.Password)
                    request.addRequestHeader("Authorization", credential)
                }
            }
            Log.i(LOGCAT_TAG, HttpHelper.deviceUA + " " + referer + " " + cookie)
            val downloadID = dm.enqueue(request)
            downloadingMap[metadata.VSID] = downloadID
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (downloadingMap.containsKey(metadata.VSID)) {
                        val query = DownloadManager.Query()
                        val cursor: Cursor = dm.query(query.setFilterById(downloadingMap[metadata.VSID]!!))
                        if (cursor.moveToFirst()) {
                            val bytesDownloaded: Long =
                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                            val bytesTotal: Long =
                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            cursor.close()
                            handlerMap[metadata.VSID]?.second?.invoke(bytesDownloaded, bytesTotal)
                            handler.postDelayed(this, CALLBACK_INTERVAL)
                        } else {
                            Log.e(LOGCAT_TAG, "Cannot move cursor")
                        }
                    }
                }
            }, CALLBACK_INTERVAL)
            Log.i(LOGCAT_TAG, "Download started " + downloadID)
            return true
        } catch (ex: Exception) {
            Log.i(LOGCAT_TAG, ex.message ?: "")
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
            handlerMap[metadata.VSID]?.first?.invoke(false)
            Log.i(LOGCAT_TAG, "Download aborted " + metadata.VSID)
            return true
        } catch (ex: Exception) {
            ex.printStackTrace()
            return false
        }
    }

    fun isDownloading(metadata: PackageMetadata): Boolean {
        return downloadingMap.containsKey(metadata.VSID)
    }

    fun startSelfUpdateDownload(url: String, failureCallback: (String) -> Unit): Boolean {
        if (downloadingMap.containsKey(MAGIC_UPDATE.VSID)) return true
        Log.i(LOGCAT_TAG, "Not in VSID-db, starting")
        try {
            setHandler(MAGIC_UPDATE, {
                if (it) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                        FileProvider.getUriForFile(
                            ApplicationContext.context,
                            ApplicationContext.context.packageName + ".provider",
                            PackLocalManager.getUpdateTempFile()
                        )
                    } else {
                        Uri.fromFile(PackLocalManager.getUpdateTempFile())
                    }
                    Log.i(LOGCAT_TAG, uri.toString())
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    ApplicationContext.context.startActivity(intent)
                    exitProcess(0)
                } else {
                    failureCallback("Error during update")
                    setHandler(MAGIC_UPDATE, null, null)
                }
            }, null)
            return startDownload(MAGIC_UPDATE, url)
        } catch (ex: Exception) {
            Log.e(LOGCAT_TAG, "Download Fail", ex)
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