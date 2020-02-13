package tk.zbx1425.bvecontentservice.ui.component

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Html
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.preference.PreferenceManager
import okhttp3.Credentials
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.model.AuthorMetadata
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata
import tk.zbx1425.bvecontentservice.log.Log
import tk.zbx1425.bvecontentservice.ui.activity.PackDetailActivity
import java.net.URL
import java.util.*

class DescriptionView(context: Context) : FrameLayout(context) {

    constructor(context: Context, metadata: PackageMetadata) :
            this(context, metadata.Description, metadata.Source)

    constructor(context: Context, metadata: AuthorMetadata) :
            this(context, metadata.Description, metadata.Source)

    constructor(context: Context, url: String, source: SourceMetadata) : this(context) {
        val textDescription = TextView(context)
        textDescription.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                "useWebView", false
            )
        ) {
            if (url.trim().startsWith("http://") ||
                url.trim().startsWith("https://")
            ) {
                val webView = WebView(context)
                webView.settings.javaScriptEnabled =
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .getBoolean(
                            "enableJavascript", true
                        )
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        webView.loadUrl("javascript:bcs.resize(document.body.getBoundingClientRect().height+10)")
                        super.onPageFinished(view, url)
                    }
                }
                webView.addJavascriptInterface(object : PackDetailActivity.ResizeInterface() {
                    @JavascriptInterface
                    override fun resize(height: Float) {
                        (context as Activity).runOnUiThread {
                            webView.layoutParams = LayoutParams(
                                LayoutParams.MATCH_PARENT,
                                (height * resources.displayMetrics.density).toInt()
                            )
                        }
                    }
                }, "bcs")
                this.addView(webView)
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
                this.addView(textDescription)
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
                            connection.addRequestProperty("Authorization", credential)
                        }
                    }
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
            if (url.trim().startsWith("http://") ||
                url.trim().startsWith("https://")
            ) {
                textDescription.text = resources.getText(R.string.info_fetch_text)
                Thread {
                    try {
                        val result =
                            HttpHelper.fetchNonapiString(source, url)
                        Log.i("BCSDescription", url)
                        Log.i("BCSDescription", result ?: "")
                        val spanned =
                            if (url.trim().toLowerCase(Locale.US).endsWith(".html")) {
                                if (android.os.Build.VERSION.SDK_INT > 24) {
                                    Html.fromHtml(
                                        result, Html.FROM_HTML_MODE_COMPACT,
                                        imageGetter, null
                                    )
                                } else {
                                    Html.fromHtml(result, imageGetter, null)
                                }
                            } else {
                                result
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
            this.addView(textDescription)
        }
    }


}