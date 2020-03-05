package tk.zbx1425.bvecontentservice.ui

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.io.log.Log
import tk.zbx1425.bvecontentservice.nullify

open class InterceptedWebViewClient : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            if (!HttpHelper.shouldInterceptRequest(request.url.toString())) return null
            return try {
                val builder = HttpHelper.getBasicBuilder(request.url.toString(), true)
                request.requestHeaders.forEach { builder.header(it.key, it.value) }
                val response = HttpHelper.client.newCall(builder.build()).execute()
                WebResourceResponse(
                    response.header("Content-Type")?.substringBefore(";")
                        ?: ApplicationContext.context.contentResolver.getType(request.url) ?: "text/html",
                    response.header("Content-Type")?.substringAfter("charset=", "")?.nullify()?.trim()
                        ?: response.header("Content-Encoding") ?: "UTF-8", response.body()?.byteStream()
                )
            } catch (ex: Exception) {
                Log.i("BCSWebView", "Non-fatal network fault: " + ex.message)
                WebResourceResponse("", "", null)
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        if (!HttpHelper.shouldInterceptRequest(url)) return null
        return try {
            val builder = HttpHelper.getBasicBuilder(url, true)
            val response = HttpHelper.client.newCall(builder.build()).execute()
            WebResourceResponse(
                response.header("Content-Type")?.substringBefore(";")
                    ?: ApplicationContext.context.contentResolver.getType(Uri.parse(url)) ?: "text/html",
                response.header("Content-Type")?.substringAfter("charset=", "")?.nullify()?.trim()
                    ?: response.header("Content-Encoding") ?: "UTF-8", response.body()?.byteStream()
            )
        } catch (ex: Exception) {
            Log.i("BCSWebView", "Non-fatal network fault: " + ex.message)
            WebResourceResponse("", "", null)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        view?.loadUrl("javascript:bcs.resize(document.body.getBoundingClientRect().height)")
        super.onPageFinished(view, url)
    }
}