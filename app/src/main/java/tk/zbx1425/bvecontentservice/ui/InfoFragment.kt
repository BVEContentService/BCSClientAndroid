package tk.zbx1425.bvecontentservice.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_webview.view.*
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.log.Log

class InfoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i("BCSUi", "onCreateView called")
        val newView = inflater.inflate(R.layout.activity_webview, container, false)
        newView.continueButton.visibility = View.GONE
        newView.webView.settings.javaScriptEnabled =
            PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context).getBoolean(
                "enableJavascript", true
            )
        newView.webView.loadUrl(MetadataManager.indexHomepage)
        newView.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                newView.loadingProgress.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                newView.loadingProgress.visibility = View.GONE
                super.onPageFinished(view, url)
            }
        }
        return newView
    }
}