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

package tk.zbx1425.bvecontentservice.ui.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_webview.*
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.io.log.Log
import java.net.URL
import java.net.URLDecoder


class ForceViewActivity : AppCompatActivity() {

    lateinit var metadata: PackageMetadata
    var guidedDownload: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        metadata = intent.getSerializableExtra("metadata") as PackageMetadata
        guidedDownload = metadata.GuidedDownload
        if (metadata.ForceView || metadata.NoFile || guidedDownload) continueButton.visibility = View.GONE
        continueButton.setOnClickListener {
            setResult(Activity.RESULT_OK, Intent().putExtra("url", metadata.File))
            finish()
        }
        webView.settings.javaScriptEnabled = getPreference("enableJavascript", true)
        if (guidedDownload) {
            webView.loadUrl(if (metadata.File_REL != "") metadata.File; else metadata.Homepage)
        } else {
            webView.loadUrl(metadata.Homepage)
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                setTitle(title)
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loadingProgress.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loadingProgress.visibility = View.GONE
                super.onPageFinished(view, url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String): Boolean {
                if (url == resources.getString(R.string.url_start_download)) {
                    if (!metadata.NoFile) {
                        setResult(Activity.RESULT_OK, Intent().putExtra("url", metadata.File))
                        finish()
                    }
                    return true
                }
                return super.shouldOverrideUrlLoading(view, url)
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    if (!HttpHelper.shouldInterceptRequest(request.url.toString())) return null
                    val builder = HttpHelper.getBasicBuilder(request.url.toString(), true)
                    request.requestHeaders.forEach { builder.header(it.key, it.value) }
                    val response = HttpHelper.client.newCall(builder.build()).execute()
                    return WebResourceResponse(
                        response.header("Content-Type")?.substringBefore(";")
                            ?: this@ForceViewActivity.contentResolver.getType(request.url) ?: "text/html",
                        response.header("Content-Type")?.substringAfter("charset=")?.trim()
                            ?: response.header("Content-Encoding") ?: "UTF-8", response.body()?.byteStream()
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                val builder = HttpHelper.getBasicBuilder(url, true)
                val response = HttpHelper.client.newCall(builder.build()).execute()
                Log.i("BCSWebView", "LegacyCall")
                return WebResourceResponse(
                    response.header("Content-Type")?.substringBefore(";")
                        ?: this@ForceViewActivity.contentResolver.getType(Uri.parse(url)) ?: "text/html",
                    response.header("Content-Type")?.substringAfterLast(";")?.trim()
                        ?: response.header("Content-Encoding") ?: "UTF-8", response.body()?.byteStream()
                )
            }
        }
        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            Log.i("BCSWebView", "DownloadListener Called")
            if (!guidedDownload) return@setDownloadListener
            var fileName = URLDecoder.decode(
                contentDisposition.replaceFirst(
                    "(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1"
                ), "UTF-8"
            )
            if (fileName == "") fileName = URL(url).path
            if (!fileName.endsWith(".zip", true)) return@setDownloadListener
            /*val request = Request.Builder().url(url)
                .addHeader("cookie", CookieManager.getInstance().getCookie(url))
                .addHeader("User-Agent", userAgent)
                .addHeader("Referer", webView.url).build()
            Thread {
                try {
                    val response = HttpHelper.client.newCall(request).execute()
                    val bs = response.body()?.byteStream() ?: return@Thread
                    val input = BufferedInputStream(bs)
                    val output: OutputStream = FileOutputStream(File(PackLocalManager.appDir, "test.zip"))
                    val data = ByteArray(1024)
                    var total: Long = 0
                    var count: Int
                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()
                    Log.i("BCSDownload", "Finished Download")
                } catch (ex: Exception) {
                    Log.e("BCSDownload", "Fail Download", ex)
                }
            }.start()*/

            //onDownloadStartNoStream(this, url, userAgent, contentDisposition, mimetype, webView.url)
            setResult(
                Activity.RESULT_OK, Intent().putExtra("url", url)
                    .putExtra("cookie", CookieManager.getInstance().getCookie(url))
                    .putExtra("referer", webView.url)
            )
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}