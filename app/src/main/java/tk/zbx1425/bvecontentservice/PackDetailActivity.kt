package tk.zbx1425.bvecontentservice

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_COMPACT
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_pack_detail.*
import okhttp3.Credentials
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import tk.zbx1425.bvecontentservice.storage.PackDownloadManager
import tk.zbx1425.bvecontentservice.storage.PackListManager
import tk.zbx1425.bvecontentservice.storage.PackLocalManager
import tk.zbx1425.bvecontentservice.storage.PackLocalManager.removeLocalPacks
import tk.zbx1425.bvecontentservice.ui.ImageLoader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.abs


class PackDetailActivity : AppCompatActivity() {

    var isDownloadBtnShown: Boolean = false
    lateinit var metadata: PackageMetadata
    var timer: Timer = Timer()
    var timerRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pack_detail)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        //region Uninteresting silly one-by-one content setting, fucking ungraceful
        metadata = intent.getSerializableExtra("metadata") as PackageMetadata
        toolbar_layout.title = metadata.Name
        textPackName.text = metadata.Name

        app_bar.addOnOffsetChangedListener(object : AppBarLayout.OnOffsetChangedListener {
            override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
                if (abs(verticalOffset) >= appBarLayout.totalScrollRange) {
                    if (!isDownloadBtnShown) {
                        invalidateOptionsMenu()
                        isDownloadBtnShown = true
                    }
                } else if (isDownloadBtnShown) {
                    invalidateOptionsMenu()
                    isDownloadBtnShown = false
                }
            }
        })

        setButtonState()
        textAuthor.text = metadata.Author.Name
        textVersion.text = metadata.Version.get()
        textDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(metadata.Timestamp)
        textContact.text = metadata.Author.ID
        if (metadata.Homepage == "" && metadata.Author.Homepage == "") {
            rowHomepage.visibility = View.GONE
        } else if (metadata.Homepage == "") {
            textHomepage.text = metadata.Author.Homepage
            textHomepage2.visibility = View.GONE
        } else if (metadata.Author.Homepage == "") {
            textHomepage.text = metadata.Homepage
            textHomepage2.visibility = View.GONE
        } else {
            textHomepage.text = metadata.Homepage
            textHomepage2.text = metadata.Author.Homepage
        }
        textSourceAPIURL.text = metadata.Source.APIURL
        textSourceName.text = metadata.Source.Name
        textSourceMaintainer.text = metadata.Source.Author
        textSourceContact.text = metadata.Source.Contact
        if (metadata.Source.Homepage == "") {
            rowSourceHomepage.visibility = View.GONE
        } else {
            textSourceHomepage.text = metadata.Source.Homepage
        }
        textIndexAPIURL.text = metadata.Source.Index.APIURL
        textIndexName.text = metadata.Source.Index.Name
        textIndexMaintainer.text = metadata.Source.Index.Author
        textIndexContact.text = metadata.Source.Index.Contact
        if (metadata.Source.Index.Homepage == "") {
            rowIndexHomepage.visibility = View.GONE
        } else {
            textIndexHomepage.text = metadata.Source.Index.Homepage
        }
        ImageLoader.setPackImageAsync(thumbnailView, metadata)
        //endregion
        if (PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context).getBoolean(
                "useWebView", false
            )
        ) {
            if (metadata.Description.trim().startsWith("http://") ||
                metadata.Description.trim().startsWith("https://")
            ) {
                val parent: ViewGroup = textDescription.parent as ViewGroup
                val index = parent.indexOfChild(textDescription)
                parent.removeViewAt(index)
                val webView = WebView(this)
                webView.settings.javaScriptEnabled =
                    PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context)
                        .getBoolean(
                            "enableJavascript", true
                        )
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        webView.loadUrl("javascript:bcs.resize(document.body.getBoundingClientRect().height+10)")
                        super.onPageFinished(view, url)
                    }
                }
                webView.addJavascriptInterface(object : ResizeInterface() {
                    @JavascriptInterface
                    override fun resize(height: Float) {
                        runOnUiThread {
                            webView.layoutParams = LinearLayout.LayoutParams(
                                resources.displayMetrics.widthPixels,
                                (height * resources.displayMetrics.density).toInt()
                            )
                        }
                    }
                }, "bcs")
                parent.addView(webView, index)
                if (metadata.Source.APIType == "httpBasicAuth") {
                    val credential: String =
                        Credentials.basic(metadata.Source.Username, metadata.Source.Password)
                    webView.loadUrl(
                        metadata.Description.trim(),
                        mapOf(Pair("Authorization", credential))
                    )
                } else {
                    webView.loadUrl(metadata.Description.trim())
                }
            } else {
                textDescription.text = metadata.Description
            }
        } else {
            val imageGetter = Html.ImageGetter { source: String ->
                try {
                    val drawable: Drawable
                    val connection = URL(source).openConnection()
                    when (metadata.Source.APIType) {
                        "httpBasicAuth" -> {
                            val credential: String =
                                Credentials.basic(
                                    metadata.Source.Username,
                                    metadata.Source.Password
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
                    ex.printStackTrace()
                    null
                }
            }
            if (metadata.Description.trim().startsWith("http://") ||
                metadata.Description.trim().startsWith("https://")
            ) {
                textDescription.text = resources.getText(R.string.info_fetch_text)
                Thread {
                    try {
                        val result =
                            HttpHelper.fetchNonapiString(metadata.Source, metadata.Description)
                        Log.i("BCSDescription", metadata.Description)
                        Log.i("BCSDescription", result)
                        val spanned =
                            if (metadata.Description.trim().toLowerCase(Locale.US).endsWith(".html")) {
                                if (android.os.Build.VERSION.SDK_INT > 24) {
                                    Html.fromHtml(
                                        result, FROM_HTML_MODE_COMPACT,
                                        imageGetter, null
                                    )
                                } else {
                                    Html.fromHtml(result, imageGetter, null)
                                }
                            } else {
                                result
                            }
                        runOnUiThread {
                            textDescription.text = spanned
                        }
                    } catch (ex: Exception) {
                        runOnUiThread {
                            ex.printStackTrace()
                            textDescription.text = String.format(
                                resources.getText(R.string.info_fetch_text_fail)
                                    .toString(), metadata.Description.trim(), ex.message
                            )
                        }
                    }
                }.start()
            } else {
                textDescription.text = metadata.Description
            }
        }

        fab.setOnClickListener { startDownload() }
        downloadButton.setOnClickListener { startDownload() }
        if (MetadataManager.getActiveUGCServer() == null) ugcButton.visibility = View.GONE
        ugcButton.setOnClickListener {
            val intent = Intent(this as Context, UGCActivity::class.java)
            intent.putExtra("metadata", metadata)
            startActivity(intent)
        }
    }

    abstract class ResizeInterface {
        @JavascriptInterface
        abstract fun resize(height: Float)
    }

    override fun onDestroy() {
        timer.cancel(); timer.purge()
        Log.i("BCSUi", "Timer stopped")
        super.onDestroy()
    }

    private fun startDownload() {
        Log.i("BCSUi", "startDownload called")
        val packState = PackLocalManager.getLocalState(metadata)
        val dlgAlert = AlertDialog.Builder(this)
        dlgAlert.setNegativeButton(android.R.string.no, null)
        dlgAlert.setCancelable(true)
        dlgAlert.setTitle(R.string.app_name)
        setButtonState()
        if (packState > 100) {
            dlgAlert.setMessage(
                String.format(
                    resources.getString(R.string.alert_remove),
                    metadata.Name
                )
            )
            dlgAlert.setPositiveButton(android.R.string.yes) { _: DialogInterface, i: Int ->
                if (i == DialogInterface.BUTTON_POSITIVE) {
                    removeLocalPacks(metadata.ID)
                    PackListManager.populate()
                    finish()
                }
            }
            dlgAlert.create().show()
        } else if (packState >= 0) {
            dlgAlert.setMessage(
                String.format(
                    resources.getString(R.string.alert_abort),
                    metadata.Name
                )
            )
            dlgAlert.setPositiveButton(android.R.string.yes) { _: DialogInterface, i: Int ->
                if (i == DialogInterface.BUTTON_POSITIVE) {
                    PackDownloadManager.abortDownload(metadata)
                }
            }
            dlgAlert.create().show()
        } else {
            if (metadata.AutoOpen) {
                val intent = Intent(this as Context, ForceViewActivity::class.java)
                intent.putExtra("metadata", metadata)
                startActivityForResult(intent, 9376)
            } else {
                if (PackDownloadManager.startDownload(metadata)) {
                    setResult(Activity.RESULT_OK, null)
                    timer = Timer()
                    timer.schedule(timerTask { setButtonState(true) }, 500, 500)
                } else {
                    Toast.makeText(
                        this as Context, ApplicationContext.context.resources.getString(
                            R.string.info_download_start_failed
                        ), Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 9376 && resultCode == Activity.RESULT_OK) {
            timer = Timer()
            timer.schedule(timerTask { setButtonState(true) }, 500, 500)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setButtonState(onTimer: Boolean = false) {
        if (onTimer) timerRunning = true
        runOnUiThread {
            val packState = PackLocalManager.getLocalState(metadata)
            downloadButton.text =
                when {
                    packState < 0 -> String.format(
                        resources.getString(R.string.text_download),
                        metadata.FileSize
                    )
                    packState < 100 -> resources.getString(R.string.text_downloading)
                    packState == 100 -> resources.getString(R.string.text_finishing)
                    packState > 100 -> resources.getString(R.string.text_remove)
                    else -> resources.getString(R.string.dummy)
                }
            val animation = ObjectAnimator.ofInt(
                downloadProgress,
                "progress", when {
                    packState < 0 -> 0
                    packState <= 100 -> packState
                    packState > 100 -> 0
                    else -> 0
                }
            )
            animation.duration = 200
            animation.interpolator = DecelerateInterpolator()
            animation.start()
            when {
                packState <= 100 -> downloadProgress.secondaryProgress = 0
                packState > 100 -> downloadProgress.secondaryProgress = 100
            }
            Log.i("BCSUi", "PackState: " + packState)
            if ((packState > 100 || packState < 0) && timerRunning) {
                timer.cancel(); timer.purge()
                Log.i("BCSUi", "Timer stopped")
                timerRunning = false
            } else if (packState >= 0 && !timerRunning) {
                timer = Timer()
                timer.schedule(timerTask { setButtonState(true) }, 0, 500)
                Log.i("BCSUi", "Timer started")
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_pack_detail, menu)
        val item: MenuItem = menu!!.findItem(R.id.action_download)
        if (!isDownloadBtnShown) item.isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_download -> {
                startDownload()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}