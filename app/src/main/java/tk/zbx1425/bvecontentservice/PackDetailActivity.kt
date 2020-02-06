package tk.zbx1425.bvecontentservice

import android.animation.ObjectAnimator
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.text.Html.FROM_HTML_MODE_COMPACT
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_pack_detail.*
import okhttp3.Request
import tk.zbx1425.bvecontentservice.api.DownloadImageTask
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import tk.zbx1425.bvecontentservice.storage.PackDownloadManager
import tk.zbx1425.bvecontentservice.storage.PackListManager
import tk.zbx1425.bvecontentservice.storage.PackLocalManager
import tk.zbx1425.bvecontentservice.storage.PackLocalManager.removeLocalPacks
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.abs


class PackDetailActivity : AppCompatActivity() {

    var isDownloadBtnShown: Boolean = false
    lateinit var metadata: PackageMetadata
    val timer = Timer()
    var timerRunning: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pack_detail)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        metadata = intent.getSerializableExtra("metadata") as PackageMetadata
        toolbar_layout.title = metadata.Name_LO
        textPackName.text = metadata.Name_LO

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
        textAuthor.text = metadata.Author.Name_LO
        textVersion.text = metadata.Version.get()
        textDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .format(metadata.Timestamp)
        textContact.text = metadata.Author.ID
        if (metadata.Homepage == "" && metadata.Author.Homepage == "") {
            rowHomepage.visibility = View.GONE
        } else if (metadata.Homepage == "") {
            textHomepage.text = metadata.Homepage
            textHomepage2.visibility = View.GONE
        } else if (metadata.Author.Homepage == "") {
            textHomepage.text = metadata.Author.Homepage
            textHomepage2.visibility = View.GONE
        } else {
            textHomepage.text = metadata.Homepage
            textHomepage2.text = metadata.Author.Homepage
        }
        textSourceAPIURL.text = metadata.Source.APIURL
        textSourceName.text = metadata.Source.Name_LO
        textSourceMaintainer.text = metadata.Source.Maintainer
        textSourceContact.text = metadata.Source.Contact
        if (metadata.Source.Homepage == "") {
            rowSourceHomepage.visibility = View.GONE
        } else {
            textSourceHomepage.text = metadata.Source.Homepage
        }
        textIndexAPIURL.text = metadata.Source.Index.APIURL
        textIndexName.text = metadata.Source.Index.Name_LO
        textIndexMaintainer.text = metadata.Source.Index.Maintainer
        textIndexContact.text = metadata.Source.Index.Contact
        if (metadata.Source.Index.Homepage == "") {
            rowIndexHomepage.visibility = View.GONE
        } else {
            textIndexHomepage.text = metadata.Source.Index.Homepage
        }
        DownloadImageTask(thumbnailView).execute(metadata.Thumbnail)

        val imageGetter = Html.ImageGetter { source: String ->
            try {
                val drawable: Drawable
                val url = URL(source)
                drawable = Drawable.createFromStream(url.openStream(), "")
                drawable.setBounds(
                    0, 0, drawable.intrinsicWidth, drawable
                        .intrinsicHeight
                )
                drawable
            } catch (ex: Exception) {
                null
            }
        }
        if (metadata.Description.trim().startsWith("http://") ||
            metadata.Description.trim().startsWith("https://")
        ) {
            textDescription.text = resources.getText(R.string.info_fetch_text)
            Thread {
                try {
                    Log.i("BCSDescription", metadata.Description.trim())
                    val request = Request.Builder().url(metadata.Description.trim()).build()
                    val response = MetadataManager.client.newCall(request).execute()
                    val result = response.body()?.string() ?: ""
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

        fab.setOnClickListener { startDownload() }
        downloadButton.setOnClickListener { startDownload() }
    }

    override fun onDestroy() {
        timer.cancel(); timer.purge()
        super.onDestroy()
    }

    private fun startDownload() {
        val packState = PackLocalManager.getLocalState(metadata)
        val dlgAlert = AlertDialog.Builder(this)
        dlgAlert.setNegativeButton(android.R.string.no, null)
        dlgAlert.setCancelable(true)
        dlgAlert.setTitle(R.string.app_name)
        if (packState > 100) {
            dlgAlert.setMessage(
                String.format(
                    resources.getString(R.string.alert_remove),
                    metadata.Name_LO
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
                    metadata.Name_LO
                )
            )
            dlgAlert.setPositiveButton(android.R.string.yes) { _: DialogInterface, i: Int ->
                if (i == DialogInterface.BUTTON_POSITIVE) {
                    if (PackDownloadManager.startDownload(metadata)) {
                        Snackbar.make(
                            app_bar,
                            R.string.info_download_aborted, Snackbar.LENGTH_SHORT
                        ).show()
                        PackDownloadManager.abortDownload(metadata)
                    } else {
                        Snackbar.make(
                            app_bar,
                            R.string.info_download_abort_failed, Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            dlgAlert.create().show()
        } else {
            if (PackDownloadManager.startDownload(metadata)) {
                Snackbar.make(
                    app_bar,
                    R.string.info_download_started, Snackbar.LENGTH_SHORT
                ).show()
                setButtonState()
            } else {
                Snackbar.make(
                    app_bar,
                    R.string.info_download_start_failed, Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setButtonState() {
        runOnUiThread {
            val packState = PackLocalManager.getLocalState(metadata)
            downloadButton.text = resources.getString(
                when {
                    packState < 0 -> R.string.text_download
                    packState < 100 -> R.string.text_downloading
                    packState == 100 -> R.string.text_finishing
                    packState > 100 -> R.string.text_remove
                    else -> R.string.dummy
                }
            )
            val animation = ObjectAnimator.ofInt(
                downloadProgress,
                "progress", when {
                    packState < 0 -> 0
                    packState <= 100 -> packState
                    packState > 100 -> 0
                    else -> 0
                }
            )
            animation.duration = 1000 // 1 second
            animation.interpolator = DecelerateInterpolator()
            animation.start()
            when {
                packState <= 100 -> downloadProgress.secondaryProgress = 0
                packState > 100 -> downloadProgress.secondaryProgress = 100
            }
            if (packState > 100) {
                timer.cancel(); timer.purge()
                timerRunning = false
            } else if (packState >= 0 && !timerRunning) {
                timer.schedule(timerTask { setButtonState() }, 0, 1000)
                timerRunning = true
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