package tk.zbx1425.bvecontentservice

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_webview.*
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.PackageMetadata


class UGCActivity : AppCompatActivity() {

    lateinit var metadata: PackageMetadata

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        if (MetadataManager.getActiveUGCServer() == null) {
            finish()
            return
        }
        metadata = intent.getSerializableExtra("metadata") as PackageMetadata
        continueButton.visibility = View.GONE
        webView.settings.javaScriptEnabled =
            PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context).getBoolean(
                "enableJavascript", true
            )
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.setSupportMultipleWindows(true)
        webView.loadUrl(
            String.format(
                "%s?pkg=%s&ver=%s",
                MetadataManager.getActiveUGCServer()?.APIURL,
                metadata.ID, metadata.Version.get()
            )
        )
        webView.webChromeClient = MultiPageChromeClient(this)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loadingProgress.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loadingProgress.visibility = View.GONE
                super.onPageFinished(view, url)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }

    class MultiPageChromeClient(val activity: UGCActivity) : WebChromeClient() {
        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            activity.title = title
        }

        @JavascriptInterface
        override fun onCreateWindow(
            view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?
        ): Boolean {
            Log.i("BCSWeb", "CreateWindow Called")
            val webView = WebView(activity)
            webView.settings.javaScriptEnabled =
                PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context)
                    .getBoolean(
                        "enableJavascript", true
                    )
            webView.settings.javaScriptCanOpenWindowsAutomatically = true
            webView.settings.setSupportMultipleWindows(true)
            activity.layoutFrame.addView(webView)
            val transport =
                resultMsg!!.obj as WebViewTransport
            transport.webView = webView
            resultMsg.sendToTarget()
            webView.webViewClient = WebViewClient()
            webView.webChromeClient = MultiPageChromeClient(activity)
            return true
        }

        @JavascriptInterface
        override fun onCloseWindow(window: WebView?) {
            super.onCloseWindow(window)
            if (window != null) {
                activity.layoutFrame.removeView(window)
                window.destroy()
            }
        }
    }
}