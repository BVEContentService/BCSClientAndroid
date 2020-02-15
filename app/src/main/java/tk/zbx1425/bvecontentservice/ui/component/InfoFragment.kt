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

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.activity_webview.view.*
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.getPreference


class InfoFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val newView = inflater.inflate(R.layout.activity_webview, container, false)
        newView.continueButton.visibility = View.GONE
        newView.webView.settings.javaScriptEnabled = getPreference("enableJavascript", true)
        newView.webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        if (MetadataManager.indexHomepage != "") {
            newView.webView.loadUrl(MetadataManager.indexHomepage)
        } else {
            newView.webView.loadData(
                "<table style='width:100%;height:100%'><td style='text-align:center;vertical-align:middle'>" +
                        resources.getString(R.string.text_noinfo) + "</td></table>",
                "text/html", "UTF-8"
            )
        }
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