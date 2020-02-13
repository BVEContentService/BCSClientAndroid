package tk.zbx1425.bvecontentservice.storage

import android.widget.Toast
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.StatusUtil
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.kotlin.listener.createListener2
import okhttp3.Credentials
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import tk.zbx1425.bvecontentservice.log.Log
import java.io.*


object PackDownloadManager {

    const val LOGCAT_TAG = "BCSDownloadManager"
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