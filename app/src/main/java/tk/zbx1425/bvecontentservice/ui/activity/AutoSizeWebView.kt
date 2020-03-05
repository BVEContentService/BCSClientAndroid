package tk.zbx1425.bvecontentservice.ui.activity

import android.content.Context
import android.graphics.Canvas
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView


class AutoSizeWebView : WebView {
    private var lastContentHeight = 0

    constructor(context: Context) : super(context) {
        this.addJavascriptInterface(this, "bcs")
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        this.measure(0, 0)
        val measuredHeight: Int = this.measuredHeight
        if (measuredHeight != lastContentHeight) {
            val layoutParams: ViewGroup.LayoutParams = this.layoutParams
            layoutParams.height = measuredHeight
            this.layoutParams = layoutParams
            lastContentHeight = measuredHeight
        }
    }

    @JavascriptInterface
    fun resize(height: Float) {
        /*(context as Activity).runOnUiThread{
            this.layoutParams = FrameLayout.LayoutParams(LayoutParams(resources.displayMetrics.widthPixels,
                (height * resources.displayMetrics.density).toInt()))
        }*/
    }
}