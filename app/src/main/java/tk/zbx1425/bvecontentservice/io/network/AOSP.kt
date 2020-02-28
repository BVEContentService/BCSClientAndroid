package tk.zbx1425.bvecontentservice.io.network

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.URLUtil.guessFileName
import android.widget.Toast
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


/**
 * Notify the host application a download should be done, even if there
 * is a streaming viewer available for thise type.
 * @param activity Activity requesting the download.
 * @param url The full url to the content that should be downloaded
 * @param userAgent User agent of the downloading application.
 * @param contentDisposition Content-disposition http header, if present.
 * @param mimetype The mimetype of the content reported by the server
 * @param referer The referer associated with the downloaded url
 * @param privateBrowsing If the request is coming from a private browsing tab.
 */
/*package */
fun onDownloadStartNoStream(
    activity: Activity,
    url: String, userAgent: String?, contentDisposition: String?,
    mimetype: String?, referer: String?
) {
    val filename: String = guessFileName(
        url,
        contentDisposition, mimetype
    )
    // Check to see if we have an SDCard
    val status: String = Environment.getExternalStorageState()
    if (status != Environment.MEDIA_MOUNTED) {
        val title: String
        val msg: String
        // Check to see if the SDCard is busy, same as the music app
        if (status == Environment.MEDIA_SHARED) {
            msg = "R.string.download_sdcard_busy_dlg_msg"
            title = "R.string.download_sdcard_busy_dlg_title"
        } else {
            msg = "R.string.download_no_sdcard_dlg_msg" + filename
            title = "R.string.download_no_sdcard_dlg_title"
        }
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(msg)
            .setPositiveButton("R.string.ok", null)
            .show()
        return
    }
    val addressString: String = url
    val uri: Uri = Uri.parse(addressString)
    val request: DownloadManager.Request
    try {
        request = DownloadManager.Request(uri)
    } catch (e: IllegalArgumentException) {
        Toast.makeText(activity, "CANNOT DOWNLOAD", Toast.LENGTH_SHORT).show()
        return
    }
    request.setMimeType(mimetype)
    // set downloaded file destination to /sdcard/Download.
// or, should it be set to one of several Environment.DIRECTORY* dirs depending on mimetype?
    try {
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
    } catch (ex: IllegalStateException) { // This only happens when directory Downloads can't be created or it isn't a directory
// this is most commonly due to temporary problems with sdcard so show appropriate string
        Toast.makeText(
            activity, "download_sdcard_busy_dlg_title",
            Toast.LENGTH_SHORT
        ).show()
        return
    }
    // let this downloaded file be scanned by MediaScanner - so that it can
// show up in Gallery app, for example.
    request.allowScanningByMediaScanner()
    // XXX: Have to use the old url since the cookies were stored using the
// old percent-encoded url.
    val cookies: String = CookieManager.getInstance().getCookie(url)
    request.addRequestHeader("cookie", cookies)
    request.addRequestHeader("User-Agent", userAgent)
    request.addRequestHeader("Referer", referer)
    request.setNotificationVisibility(
        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
    )
    if (mimetype == null) {
        if (TextUtils.isEmpty(addressString)) {
            return
        }
        FetchUrlMimeType(
            activity, request, addressString, cookies,
            userAgent
        ).start()
    } else {
        val manager: DownloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        object : Thread("Browser download") {
            override fun run() {
                manager.enqueue(request)
            }
        }.start()
    }
    Toast.makeText(activity, "string.download_pending", Toast.LENGTH_SHORT)
        .show()
}

internal class FetchUrlMimeType(
    context: Context, request: DownloadManager.Request,
    uri: String, cookies: String?, userAgent: String?
) : Thread() {
    private val mContext: Context
    private val mRequest: DownloadManager.Request
    private val mUri: String
    private val mCookies: String?
    private val mUserAgent: String?
    override fun run() {
        var mimeType: String? = null
        var contentDisposition: String? = null
        var connection: HttpURLConnection? = null
        try {
            val url = URL(mUri)
            connection = url.openConnection() as HttpURLConnection
            connection.setRequestMethod("HEAD")
            if (mUserAgent != null) {
                connection.addRequestProperty("User-Agent", mUserAgent)
            }
            if (mCookies != null && mCookies.length > 0) {
                connection.addRequestProperty("Cookie", mCookies)
            }
            if (connection.getResponseCode() == 200) {
                mimeType = connection.getContentType()
                if (mimeType != null) {
                    val semicolonIndex = mimeType.indexOf(';')
                    if (semicolonIndex != -1) {
                        mimeType = mimeType.substring(0, semicolonIndex)
                    }
                }
                contentDisposition = connection.getHeaderField("Content-Disposition")
            }
        } catch (ioe: IOException) {
            Log.e(LOGTAG, "Download failed: $ioe")
        } finally {
            if (connection != null) {
                connection.disconnect()
            }
        }
        if (mimeType != null) {
            if (mimeType.equals("text/plain", ignoreCase = true) ||
                mimeType.equals("application/octet-stream", ignoreCase = true)
            ) {
                val newMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(mUri)
                )
                if (newMimeType != null) {
                    mimeType = newMimeType
                    mRequest.setMimeType(newMimeType)
                }
            }
            val filename = guessFileName(
                mUri, contentDisposition,
                mimeType
            )
            mRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
        }
        // Start the download
        val manager = mContext.getSystemService(
            Context.DOWNLOAD_SERVICE
        ) as DownloadManager
        manager.enqueue(mRequest)
    }

    companion object {
        private const val LOGTAG = "FetchUrlMimeType"
    }

    init {
        mContext = context.applicationContext
        mRequest = request
        mUri = uri
        mCookies = cookies
        mUserAgent = userAgent
    }
}