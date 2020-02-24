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

import Identification
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import com.tonyodev.fetch2.*
import com.tonyodev.fetch2core.DownloadBlock
import com.tonyodev.fetch2core.Extras
import com.tonyodev.fetch2core.Func
import okhttp3.Credentials
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.io.throttling.ThrottledOkHttpDownloader
import tk.zbx1425.bvecontentservice.log.Log
import java.io.*
import kotlin.system.exitProcess


object PackDownloadManager {

    const val LOGCAT_TAG = "BCSDownloadManager"
    const val MAGIC_UPDATE = "__SELF_UPDATE"
    private var mapInitialized: Boolean = false
    val downloadingMap = HashMap<String, Int>()

    private val fetchConfiguration = FetchConfiguration.Builder(ApplicationContext.context)
        .setDownloadConcurrentLimit(3).enableLogging(true)
        .setHttpDownloader(ThrottledOkHttpDownloader(HttpHelper.client)).enableRetryOnNetworkGain(true)
        .build()

    private val fetchListener: FetchListener = object : FetchListener {
        override fun onAdded(download: Download) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Added")
        }

        override fun onCancelled(download: Download) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Cancelled")
            fetch.delete(download.id) //Make it simple & dirty
        }

        override fun onCompleted(download: Download) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Completed")
            Toast.makeText(
                ApplicationContext.context, String.format(
                    ApplicationContext.context.resources.getText(R.string.info_download_finished).toString(),
                    download.extras.getString("Name", ""), ""
                ), Toast.LENGTH_LONG
            ).show()
            fetch.remove(download.id)
            if (download.extras.getString("VSID", "") != MAGIC_UPDATE) {
                PackLocalManager.getLocalTempFile(download.extras.getString("VSID", "BULLSHIT"))
                    .renameTo(PackLocalManager.getLocalPackFile(download.extras.getString("VSID", "BULLSHIT")))
                PackListManager.populate()
            }
        }

        override fun onDeleted(download: Download) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Deleted")
            Toast.makeText(
                ApplicationContext.context, String.format(
                    ApplicationContext.context.resources.getText(R.string.info_download_aborted).toString(),
                    download.extras.getString("Name", ""), ""
                ), Toast.LENGTH_LONG
            ).show()
            downloadingMap.remove(download.extras.getString("VSID", ""))
            fetch.remove(download.id)
        }

        override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {

        }

        override fun onError(download: Download, error: com.tonyodev.fetch2.Error, throwable: Throwable?) {
            Toast.makeText(
                ApplicationContext.context, String.format(
                    ApplicationContext.context.resources.getText(R.string.info_download_failed).toString(),
                    download.extras.getString("Name", ""), error.throwable?.message ?: ""
                ), Toast.LENGTH_LONG
            ).show()
            Log.e(LOGCAT_TAG, download.id.toString() + " Failed")
            Log.e(LOGCAT_TAG, error.httpResponse?.errorResponse ?: "")
            fetch.delete(download.id)
        }

        override fun onPaused(download: Download) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Paused. deleting")
            fetch.delete(download.id) //Make it simple & dirty, No need to pause a download
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            Log.i(LOGCAT_TAG, download.id.toString() + " ETA(ms) " + etaInMilliSeconds)
        }

        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Queued")
        }

        override fun onRemoved(download: Download) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Removed.")
            downloadingMap.remove(download.extras.getString("VSID", ""))
            //fetch.delete(download.id) //Make it simple & dirty
        }

        override fun onResumed(download: Download) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Resumed")
        }

        override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Started")
            Toast.makeText(
                ApplicationContext.context, String.format(
                    ApplicationContext.context.resources.getText(R.string.info_download_started)
                        .toString()/*, download.extras.getString("Name", "Unknown")*/
                ), Toast.LENGTH_SHORT
            ).show()
        }

        override fun onWaitingNetwork(download: Download) {
            Log.i(LOGCAT_TAG, download.id.toString() + " Waiting for network...")
        }

    }
    val fetch = Fetch.Impl.getInstance(fetchConfiguration).addListener(fetchListener)

    fun isDownloading(metadata: PackageMetadata): Boolean {
        return downloadingMap.containsKey(metadata.VSID)
    }

    fun syncDownloadMap() {
        fetch.getDownloads(Func {
            downloadingMap.clear()
            for (download in it) {
                downloadingMap[download.extras.getString("VSID", "")] = download.id
            }
        })
    }

    fun startDownload(metadata: PackageMetadata): Boolean {
        if (downloadingMap.containsKey(metadata.VSID)) return true
        Log.i(LOGCAT_TAG, "Not in VSID-db, starting")
        try {
            PackLocalManager.ensureHmmsimDir()
            Log.i(LOGCAT_TAG, metadata.File)
            val request = Request(metadata.File, PackLocalManager.getLocalTempFile(metadata.VSID).absolutePath)
            request.addHeader("User-Agent", HttpHelper.FAKEUA)
            request.addHeader("X-BCS-UUID", Identification.deviceID)
            request.addHeader("X-BCS-CHECKSUM", Identification.getChecksum(metadata))
            request.extras = Extras(
                mapOf(
                    "VSID" to metadata.VSID, "Name" to metadata.Name,
                    "Throttle" to metadata.Source.DevSpec.Throttle.toString(),
                    "Referer" to if (metadata.Referer != "") metadata.Referer else HttpHelper.REFERER
                )
            )
            when (metadata.Source.APIType) {
                "httpBasicAuth" -> {
                    val credential: String =
                        Credentials.basic(metadata.Source.Username, metadata.Source.Password)
                    request.addHeader("Authorization", credential)
                }
            }
            fetch.enqueue(request,
                Func { updatedRequest: Request ->
                    downloadingMap[metadata.VSID] = updatedRequest.id
                },
                Func { error: com.tonyodev.fetch2.Error ->
                    throw error.throwable ?: RuntimeException("Something wrong during downloading")
                }
            )
            Log.i(LOGCAT_TAG, "Download started")
            return true
        } catch (ex: Exception) {
            Log.e(LOGCAT_TAG, "Download Fail", ex)
            ex.printStackTrace()
            return false
        }
    }

    fun startSelfUpdateDownload(url: String, failureCallback: (String) -> Unit): Boolean {
        if (downloadingMap.containsKey(MAGIC_UPDATE)) return true
        Log.i(LOGCAT_TAG, "Not in VSID-db, starting")
        try {
            PackLocalManager.ensureAppDir()
            Log.i(LOGCAT_TAG, url)
            Log.i(LOGCAT_TAG, PackLocalManager.getUpdateTempFile().absolutePath)
            if (PackLocalManager.getUpdateTempFile().exists()) PackLocalManager.getUpdateTempFile().delete()
            val request = Request(url, PackLocalManager.getUpdateTempFile().absolutePath)
            request.addHeader("User-Agent", HttpHelper.FAKEUA)
            request.addHeader("X-BCS-UUID", Identification.deviceID)
            request.extras = Extras(mapOf(Pair("VSID", MAGIC_UPDATE), Pair("Name", MAGIC_UPDATE)))
            val updateListener = object : FetchListener {
                override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {}
                override fun onAdded(download: Download) {}
                override fun onCancelled(download: Download) {}
                override fun onDeleted(download: Download) {}
                override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {}
                override fun onPaused(download: Download) {}
                override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {}
                override fun onQueued(download: Download, waitingOnNetwork: Boolean) {}
                override fun onRemoved(download: Download) {}
                override fun onResumed(download: Download) {}
                override fun onWaitingNetwork(download: Download) {}

                override fun onError(download: Download, error: com.tonyodev.fetch2.Error, throwable: Throwable?) {
                    val infoText = String.format(
                        ApplicationContext.context.resources.getText(R.string.info_download_failed).toString()
                        , "", error.throwable?.message
                    )
                    Toast.makeText(ApplicationContext.context, infoText, Toast.LENGTH_LONG).show()
                    failureCallback(infoText)
                    fetch.removeListener(this)
                }

                override fun onCompleted(download: Download) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                        FileProvider.getUriForFile(
                            ApplicationContext.context,
                            ApplicationContext.context.packageName + ".provider",
                            PackLocalManager.getUpdateTempFile())
                    } else {
                        Uri.fromFile(PackLocalManager.getUpdateTempFile())
                    }
                    Log.i(LOGCAT_TAG, uri.toString())
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    ApplicationContext.context.startActivity(intent)
                    fetch.removeListener(this)
                    exitProcess(0)
                }
            }
            fetch.addListener(updateListener)
            fetch.enqueue(request,
                Func { updatedRequest: Request ->
                    downloadingMap[MAGIC_UPDATE] = updatedRequest.id
                },
                Func { error: com.tonyodev.fetch2.Error ->
                    throw error.throwable ?: RuntimeException("Something wrong during downloading")
                }
            )
            Log.i(LOGCAT_TAG, "Download started")
            return true
        } catch (ex: Exception) {
            Log.e(LOGCAT_TAG, "Download Fail", ex)
            ex.printStackTrace()
            return false
        }
    }

    fun abortDownload(metadata: PackageMetadata): Boolean {
        if (!downloadingMap.containsKey(metadata.VSID)) return false
        try {
            //According to the documentation pausing is stopping?
            fetch.delete(downloadingMap[metadata.VSID]!!)
            downloadingMap.remove(metadata.VSID)
            Log.i(LOGCAT_TAG, "Download aborted " + metadata.VSID)
        } catch (ex: Exception) {
            Log.e(LOGCAT_TAG, "Cannot abort", ex)
            ex.printStackTrace()
            return false
        }
        return true
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