package tk.zbx1425.bvecontentservice.ui.activity

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import kotlinx.android.synthetic.main.activity_pack_detail.*
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.log.Log
import tk.zbx1425.bvecontentservice.replaceView
import tk.zbx1425.bvecontentservice.storage.ImageLoader
import tk.zbx1425.bvecontentservice.storage.PackDownloadManager
import tk.zbx1425.bvecontentservice.storage.PackListManager
import tk.zbx1425.bvecontentservice.storage.PackLocalManager
import tk.zbx1425.bvecontentservice.storage.PackLocalManager.removeLocalPacks
import tk.zbx1425.bvecontentservice.ui.component.DescriptionView
import tk.zbx1425.bvecontentservice.ui.component.MetadataView
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
        val packMetadataView = MetadataView(this, metadata, object : View.OnClickListener {
            override fun onClick(v: View?) {
                val intent = Intent(this@PackDetailActivity, AuthorActivity::class.java)
                intent.putExtra("aid", metadata.Author.ID)
                startActivity(intent)
            }
        })
        packMetadataPlaceholder.replaceView(packMetadataView)
        sourceMetadataPlaceholder.replaceView(MetadataView(this, metadata.Source))
        indexMetadataPlaceholder.replaceView(MetadataView(this, metadata.Source.Index))
        appMetadataPlaceholder.replaceView((MetadataView(this)))
        descriptionPlaceholder.replaceView(DescriptionView(this, metadata))
        ImageLoader.setPackImageAsync(thumbnailView, metadata)

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
        if (metadata.UpdateAvailable) {
            PackLocalManager.removeLocalPacks(metadata.ID)
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
        } else if (packState > 100) {
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
                    packState < 0 -> {
                        if (metadata.UpdateAvailable) {
                            String.format(
                                resources.getString(R.string.text_update),
                                metadata.FileSize
                            )
                        } else {
                            String.format(
                                resources.getString(R.string.text_download),
                                metadata.FileSize
                            )
                        }
                    }
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