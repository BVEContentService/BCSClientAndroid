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

package tk.zbx1425.bvecontentservice.ui.component

import Identification
import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.text.Html
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import okhttp3.Credentials
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.model.AuthorMetadata
import tk.zbx1425.bvecontentservice.api.model.DevSpecMetadata
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.io.hThread
import tk.zbx1425.bvecontentservice.io.log.Log
import tk.zbx1425.bvecontentservice.nullify
import tk.zbx1425.bvecontentservice.ui.InterceptedWebViewClient
import tk.zbx1425.bvecontentservice.ui.activity.AutoSizeWebView
import java.net.URL
import java.util.*


class DescriptionView(context: Context) : FrameLayout(context) {

    constructor(context: Context, metadata: PackageMetadata) :
            this(context, metadata.Description, metadata.Source)

    constructor(context: Context, metadata: AuthorMetadata) :
            this(context, metadata.Description, metadata.Source)

    constructor(context: Context, metadata: DevSpecMetadata) :
            this(context, metadata.Notice, metadata.Source ?: SourceMetadata(""))

    constructor(context: Context, url: String, source: SourceMetadata) : this(context) {
        if (url == "") return
        val textDescription = TextView(context)
        textDescription.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        this.addView(textDescription)
        if (getPreference("useWebView", true)) {
            if (url.toLowerCase(Locale.US).startsWith("http://") ||
                url.toLowerCase(Locale.US).startsWith("https://")
            ) {
                val webView = AutoSizeWebView(context)
                webView.visibility = View.GONE
                webView.settings.javaScriptEnabled = getPreference("enableJavascript", true)
                if (webView.settings.javaScriptEnabled) {
                    webView.isVerticalScrollBarEnabled = false
                    webView.isVerticalFadingEdgeEnabled = false
                    webView.isScrollbarFadingEnabled = false
                } else {
                    webView.layoutParams.height = Resources.getSystem().displayMetrics.heightPixels
                }
                webView.webViewClient = InterceptedWebViewClient()
                textDescription.text = resources.getText(R.string.info_fetch_text)
                hThread {
                    try {
                        //Fetch the response manually to bypass https limitation
                        val response = HttpHelper.getSourceClient(source).newCall(HttpHelper.getBasicBuilder(url).build()).execute()
                        (context as Activity).runOnUiThread {
                            this.addView(webView)
                            webView.loadDataWithBaseURL(
                                url,
                                response.body()?.string(),
                                response.header("Content-Type")?.substringBefore(";")
                                    ?: ApplicationContext.context.contentResolver.getType(Uri.parse(url)) ?: "text/html",
                                response.header("Content-Type")?.substringAfter("charset=", "")?.nullify()?.trim()
                                    ?: response.header("Content-Encoding") ?: "utf-8",
                                null
                            )
                            webView.visibility = View.VISIBLE
                            textDescription.visibility = View.GONE
                        }
                    } catch (ex: Exception) {
                        (context as Activity).runOnUiThread {
                            ex.printStackTrace()
                            Log.e("BCSUi", "Cannot fetch description", ex)
                            textDescription.text = String.format(
                                resources.getText(R.string.info_fetch_text_fail)
                                    .toString(), url.trim(), ex.message
                            )
                        }
                    }
                }.start()
                if (source.APIType == "httpBasicAuth") {
                    val credential: String =
                        Credentials.basic(source.Username, source.Password)
                    webView.loadUrl(
                        url.trim(),
                        mapOf(Pair("Authorization", credential))
                    )
                } else {
                    webView.loadUrl(url.trim())
                }
            } else {
                textDescription.text = url
            }
        } else {
            val imageGetter = Html.ImageGetter { src: String ->
                try {
                    val drawable: Drawable
                    val connection = URL(src).openConnection()
                    when (source.APIType) {
                        "httpBasicAuth" -> {
                            val credential: String =
                                Credentials.basic(
                                    source.Username,
                                    source.Password
                                )
                            connection.setRequestProperty("Authorization", credential)
                        }
                    }
                    connection.setRequestProperty("User-Agent", HttpHelper.deviceUA)
                    connection.setRequestProperty("Referer", HttpHelper.REFERER)
                    connection.setRequestProperty("X-BCS-UUID", Identification.deviceID)
                    connection.setRequestProperty("X-BCS-CHECKSUM", Identification.getDateChecksum())
                    drawable = Drawable.createFromStream(connection.inputStream, "")
                    drawable.setBounds(
                        0, 0, drawable.intrinsicWidth, drawable
                            .intrinsicHeight
                    )
                    drawable
                } catch (ex: Exception) {
                    Log.e("BCSUi", "Cannot get image", ex)
                    ex.printStackTrace()
                    null
                }
            }
            if (url.toLowerCase(Locale.US).startsWith("http://") ||
                url.toLowerCase(Locale.US).startsWith("https://")
            ) {
                textDescription.text = resources.getText(R.string.info_fetch_text)
                hThread {
                    try {
                        val result =
                            HttpHelper.fetchString(source, url)
                        val spanned =
                            if (url.trim().toLowerCase(Locale.US).endsWith(".txt")) {
                                result
                            } else {
                                if (Build.VERSION.SDK_INT > 24) {
                                    Html.fromHtml(
                                        result, Html.FROM_HTML_MODE_COMPACT,
                                        imageGetter, null
                                    )
                                } else {
                                    Html.fromHtml(result, imageGetter, null)
                                }
                            }
                        (context as Activity).runOnUiThread {
                            textDescription.text = spanned
                        }
                    } catch (ex: Exception) {
                        (context as Activity).runOnUiThread {
                            ex.printStackTrace()
                            Log.e("BCSUi", "Cannot fetch description", ex)
                            textDescription.text = String.format(
                                resources.getText(R.string.info_fetch_text_fail)
                                    .toString(), url.trim(), ex.message
                            )
                        }
                    }
                }.start()
            } else {
                textDescription.text = url
            }
        }
    }


}