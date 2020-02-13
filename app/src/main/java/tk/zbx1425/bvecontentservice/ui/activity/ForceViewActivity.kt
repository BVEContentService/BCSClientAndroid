package tk.zbx1425.bvecontentservice.ui.activity

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_webview.*
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.storage.PackDownloadManager

class ForceViewActivity : AppCompatActivity() {

    lateinit var metadata: PackageMetadata

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        metadata = intent.getSerializableExtra("metadata") as PackageMetadata
        if (metadata.ForceView) continueButton.visibility = View.GONE
        continueButton.setOnClickListener {
            startDownload()
        }
        webView.settings.javaScriptEnabled =
            PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context).getBoolean(
                "enableJavascript", true
            )
        webView.loadUrl(metadata.Homepage)
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
                if (url == "bcs://startDownload") {
                    startDownload()
                    return true
                }
                return super.shouldOverrideUrlLoading(view, url)
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

    fun startDownload() {
        if (PackDownloadManager.startDownload(metadata)) {
            setResult(Activity.RESULT_OK, null)
        } else {
            Toast.makeText(
                this, ApplicationContext.context.resources.getString(
                    R.string.info_download_start_failed
                ), Toast.LENGTH_SHORT
            ).show()
            setResult(Activity.RESULT_CANCELED, null)
        }
        finish()
    }
}