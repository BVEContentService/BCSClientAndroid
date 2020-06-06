package tk.zbx1425.bvecontentservice.ui.component

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout


class AutoSizeWebView : WebView {
    private var lastContentHeight = 0

    constructor(context: Context) : super(context) {
        this.addJavascriptInterface(this, "bcs")
        this.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        if (Build.VERSION.SDK_INT >= 21) {
            this.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    @JavascriptInterface
    fun resizeView(height: Float) {
        (context as Activity).runOnUiThread {
            this.layoutParams = FrameLayout.LayoutParams(
                LayoutParams(
                    resources.displayMetrics.widthPixels,
                    (height * resources.displayMetrics.density).toInt()
                )
            )
        }
    }

    @JavascriptInterface
    fun measurePage() {
        (context as Activity).runOnUiThread {
            val measureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            this.measure(measureSpec, measureSpec)
        }
    }
}