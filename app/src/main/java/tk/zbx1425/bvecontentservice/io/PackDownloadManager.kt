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
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.StatusUtil
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.kotlin.listener.createListener2
import okhttp3.Credentials
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.log.Log
import java.io.*
import kotlin.system.exitProcess


object PackDownloadManager {

    const val LOGCAT_TAG = "BCSDownloadManager"
    const val MAGIC_UPDATE = "__SELF_UPDATE"
    val downloadingMap = HashMap<String, DownloadTask>()

    fun getProgress(metadata: PackageMetadata): Int {
        if (downloadingMap.containsKey(metadata.VSID)) {
            val info = downloadingMap[metadata.VSID]!!.info
            val bytesTotal: Long = info?.totalLength ?: return -100
            if (bytesTotal > 0) {
                return (info.totalOffset * 100 / bytesTotal).toInt()
            } else {
                return 0
            }
        } else {
            return -100
        }
    }

    fun startDownload(metadata: PackageMetadata): Boolean {
        if (downloadingMap.containsKey(metadata.VSID)) return true
        Log.i(LOGCAT_TAG, "Not in VSID-db, starting")
        try {
            PackLocalManager.ensureHmmsimDir()
            Log.i(LOGCAT_TAG, metadata.File)
            val builder = DownloadTask.Builder(
                metadata.File,
                PackLocalManager.getLocalTempFile(metadata.VSID)
            )
                .setMinIntervalMillisCallbackProcess(400)
            builder.addHeader(
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.87 Safari/537.36"
            )
            builder.addHeader("X-BCS-UUID", Identification.deviceID)
            builder.addHeader("X-BCS-CHECKSUM", Identification.getChecksum(metadata))
            when (metadata.Source.APIType) {
                "httpBasicAuth" -> {
                    val credential: String =
                        Credentials.basic(metadata.Source.Username, metadata.Source.Password)
                    builder.addHeader("Authorization", credential)
                }
            }
            val task = builder.build().addTag(0, metadata.VSID).addTag(1, metadata.Name)
            downloadingMap[metadata.VSID] = task
            task.enqueue(createListener2({
                Log.i(LOGCAT_TAG, "Task started")
                Toast.makeText(
                    ApplicationContext.context, String.format(
                        ApplicationContext.context.resources.getText(R.string.info_download_started)
                            .toString(), task.getTag(1)
                    ), Toast.LENGTH_SHORT
                ).show()
            }) { dtask: DownloadTask, cause: EndCause, realCause: java.lang.Exception? ->
                Log.i(LOGCAT_TAG, "Task finished")
                Toast.makeText(
                    ApplicationContext.context, String.format(
                        ApplicationContext.context.resources.getText(
                            when (cause) {
                                EndCause.COMPLETED -> R.string.info_download_finished
                                EndCause.CANCELED -> R.string.info_download_aborted
                                else -> R.string.info_download_failed
                            }
                        ).toString(), dtask.getTag(1), when (cause) {
                            EndCause.COMPLETED, EndCause.CANCELED -> ""
                            else -> {
                                realCause?.printStackTrace(); realCause?.message ?: ""
                            }
                        }
                    ), Toast.LENGTH_LONG
                ).show()
                if (cause == EndCause.COMPLETED) {
                    PackLocalManager.getLocalTempFile(dtask.getTag(0) as String)
                        .renameTo(PackLocalManager.getLocalPackFile(dtask.getTag(0) as String))
                    PackListManager.populate()
                }
                downloadingMap.remove(dtask.getTag(0) as String)
            })
            Log.i(LOGCAT_TAG, "Download started")
            return true
        } catch (ex: Exception) {
            Log.e(LOGCAT_TAG, "Download Fail", ex)
            ex.printStackTrace()
            return false
        }
    }

    fun startSelfUpdateDownload(url: String): Boolean {
        if (downloadingMap.containsKey(MAGIC_UPDATE)) return true
        Log.i(LOGCAT_TAG, "Not in VSID-db, starting")
        try {
            PackLocalManager.ensureAppDir()
            Log.i(LOGCAT_TAG, url)
            val builder = DownloadTask.Builder(url, PackLocalManager.getUpdateTempFile())
                .setMinIntervalMillisCallbackProcess(400)
            builder.addHeader(
                "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.87 Safari/537.36"
            )
            val task = builder.build().addTag(0, MAGIC_UPDATE).addTag(1, MAGIC_UPDATE)
            downloadingMap[MAGIC_UPDATE] = task
            task.enqueue(createListener2({
                Log.i(LOGCAT_TAG, "Task started")
                Toast.makeText(
                    ApplicationContext.context, String.format(
                        ApplicationContext.context.resources.getText(R.string.info_update_start)
                            .toString(), ""
                    ), Toast.LENGTH_LONG
                ).show()
            }) { dtask: DownloadTask, cause: EndCause, realCause: java.lang.Exception? ->
                Log.i(LOGCAT_TAG, "Task finished")
                Toast.makeText(
                    ApplicationContext.context, String.format(
                        ApplicationContext.context.resources.getText(
                            when (cause) {
                                EndCause.COMPLETED -> R.string.info_download_finished
                                EndCause.CANCELED -> R.string.info_download_aborted
                                else -> R.string.info_download_failed
                            }
                        ).toString(), dtask.getTag(1), when (cause) {
                            EndCause.COMPLETED, EndCause.CANCELED -> ""
                            else -> {
                                realCause?.printStackTrace(); realCause?.message ?: ""
                            }
                        }
                    ), Toast.LENGTH_LONG
                ).show()
                if (cause == EndCause.COMPLETED) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                        FileProvider.getUriForFile(
                            ApplicationContext.context,
                            ApplicationContext.context.packageName + ".provider",
                            PackLocalManager.getUpdateTempFile())
                    } else {
                        Uri.fromFile(PackLocalManager.getUpdateTempFile())
                    }
                    intent.setDataAndType(uri, "application/vnd.android.package-archive")
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    ApplicationContext.context.startActivity(intent)
                    exitProcess(0)
                }
                downloadingMap.remove(dtask.getTag(0) as String)
            })
            Log.i(LOGCAT_TAG, "Download started")
            return true
        } catch (ex: Exception) {
            Log.e(LOGCAT_TAG, "Download Fail", ex)
            ex.printStackTrace()
            return false
        }
    }

    fun abortDownload(metadata: PackageMetadata): Boolean {
        discardCompletedTask(metadata)
        if (!downloadingMap.containsKey(metadata.VSID)) return false
        try {
            //According to the documentation pausing is stopping?
            downloadingMap[metadata.VSID]!!.cancel()
            downloadingMap.remove(metadata.VSID)
            Log.i(LOGCAT_TAG, "Download aborted " + metadata.VSID)
        } catch (ex: Exception) {
            Log.e(LOGCAT_TAG, "Cannot abort", ex)
            ex.printStackTrace()
            return false
        }
        return true
    }

    private fun discardCompletedTask(metadata: PackageMetadata) {
        if (downloadingMap.containsKey(metadata.VSID)) {
            val state = StatusUtil.isCompletedOrUnknown(downloadingMap[metadata.VSID]!!)
            if (state == StatusUtil.Status.UNKNOWN || state == StatusUtil.Status.COMPLETED)
                downloadingMap.remove(metadata.VSID)
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