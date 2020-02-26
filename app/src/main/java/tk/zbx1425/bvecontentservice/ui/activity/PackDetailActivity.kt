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

package tk.zbx1425.bvecontentservice.ui.activity

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.AppBarLayout
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Error
import com.tonyodev.fetch2.FetchListener
import com.tonyodev.fetch2core.DownloadBlock
import kotlinx.android.synthetic.main.activity_pack_detail.*
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.io.PackListManager
import tk.zbx1425.bvecontentservice.io.PackLocalManager
import tk.zbx1425.bvecontentservice.io.log.Log
import tk.zbx1425.bvecontentservice.io.network.ImageLoader
import tk.zbx1425.bvecontentservice.io.network.PackDownloadManager
import tk.zbx1425.bvecontentservice.io.network.UGCManager
import tk.zbx1425.bvecontentservice.replaceView
import tk.zbx1425.bvecontentservice.ui.component.DescriptionView
import tk.zbx1425.bvecontentservice.ui.component.MetadataView
import kotlin.math.abs


class PackDetailActivity : AppCompatActivity() {

    var isDownloadBtnShown: Boolean = false
    lateinit var metadata: PackageMetadata

    val downloadListener = object : FetchListener {
        override fun onAdded(download: Download) {
            updateUI()
        }

        override fun onCancelled(download: Download) {}
        override fun onQueued(download: Download, waitingOnNetwork: Boolean) {}
        override fun onRemoved(download: Download) {
            updateUI()
            PackDownloadManager.fetch.removeListener(this)
        }

        override fun onResumed(download: Download) {}
        override fun onDownloadBlockUpdated(download: Download, downloadBlock: DownloadBlock, totalBlocks: Int) {}
        override fun onPaused(download: Download) {}
        override fun onStarted(download: Download, downloadBlocks: List<DownloadBlock>, totalBlocks: Int) {
            updateUI()
        }

        override fun onCompleted(download: Download) {
            updateUI()
            PackDownloadManager.fetch.removeListener(this)
        }

        override fun onDeleted(download: Download) {
            updateUI()
            PackDownloadManager.fetch.removeListener(this)
        }

        override fun onError(download: Download, error: Error, throwable: Throwable?) {
            updateUI()
            PackDownloadManager.fetch.removeListener(this)
        }

        override fun onProgress(download: Download, etaInMilliSeconds: Long, downloadedBytesPerSecond: Long) {
            Log.i("BCSUi", download.progress.toString())
            runOnUiThread {
                downloadSpeed.text = String.format(
                    "%s/s",
                    android.text.format.Formatter.formatShortFileSize(this@PackDetailActivity, downloadedBytesPerSecond)
                )
                val animation = ObjectAnimator.ofInt(downloadProgress, "progress", download.progress)
                animation.duration = 200
                animation.interpolator = DecelerateInterpolator()
                animation.start()
            }
        }

        override fun onWaitingNetwork(download: Download) {}
    }

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
        if (PackDownloadManager.isDownloading(metadata)) {
            PackDownloadManager.fetch.addListener(downloadListener)
        }
        updateUI()
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
        noticePlaceholder.replaceView(DescriptionView(this, metadata.Source.DevSpec))
        descriptionPlaceholder.replaceView(DescriptionView(this, metadata))
        ImageLoader.setPackImageAsync(thumbnailView, metadata)
        Thread {
            try {
                val url = UGCManager.getURL(metadata, "query") ?: return@Thread
                val response = HttpHelper.fetchObject(url) ?: return@Thread
                val viewCount = response.getString("VIEW")
                val downloadCount = response.getString("DOWNLOAD")
                runOnUiThread {
                    textUGCView.text = viewCount
                    textUGCDownload.text = downloadCount
                    UGCLayout.visibility = View.VISIBLE
                }
            } catch (ex: Exception) {/*Pretty normal for the network to fail, isn't it?*/
            }
        }.start()

        fab.setOnClickListener { onButtonClick() }
        downloadButton.setOnClickListener { onButtonClick() }
        if (UGCManager.getActiveUGCServer() == null) {
            ugcButton.visibility = View.GONE
        }
        UGCManager.runActionAsync(metadata, "view")
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
        PackDownloadManager.fetch.removeListener(downloadListener)
        Log.i("BCSUi", "Activity Destroyed")
        super.onDestroy()
    }

    private fun onButtonClick() {
        Log.i("BCSUi", "startDownload called")
        val dlgAlert = AlertDialog.Builder(this)
        dlgAlert.setNegativeButton(android.R.string.no, null)
        dlgAlert.setCancelable(true)
        dlgAlert.setTitle(R.string.app_name)
        updateUI()
        if (metadata.UpdateAvailable) {
            PackLocalManager.removeLocalPacks(metadata.ID)
            startDownload()
        } else if (PackLocalManager.isInstalled(metadata)) {
            dlgAlert.setMessage(String.format(resources.getString(R.string.alert_remove), metadata.Name))
            dlgAlert.setPositiveButton(android.R.string.yes) { _: DialogInterface, i: Int ->
                if (i == DialogInterface.BUTTON_POSITIVE) {
                    PackLocalManager.removeLocalPacks(metadata.ID)
                    PackListManager.populate()
                    finish()
                }
            }
            dlgAlert.create().show()
        } else if (PackDownloadManager.isDownloading(metadata)) {
            dlgAlert.setMessage(String.format(resources.getString(R.string.alert_abort), metadata.Name))
            dlgAlert.setPositiveButton(android.R.string.yes) { _: DialogInterface, i: Int ->
                if (i == DialogInterface.BUTTON_POSITIVE) {
                    PackDownloadManager.abortDownload(metadata)
                }
            }
            dlgAlert.create().show()
        } else {
            if (metadata.AutoOpen || metadata.NoFile) {
                if (metadata.NoFile) UGCManager.runActionAsync(metadata, "download")
                val intent = Intent(this as Context, ForceViewActivity::class.java)
                intent.putExtra("metadata", metadata)
                startActivityForResult(intent, 9376)
            } else {
                startDownload()
            }
        }
    }

    private fun startDownload() {
        UGCManager.runActionAsync(metadata, "download")
        PackDownloadManager.fetch.addListener(downloadListener)
        if (PackDownloadManager.startDownload(metadata)) {
            setResult(Activity.RESULT_OK, null)
            updateUI()
        } else {
            PackDownloadManager.fetch.removeListener(downloadListener)
            Toast.makeText(
                this as Context, ApplicationContext.context.resources.getString(
                    R.string.info_download_start_failed
                ), Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 9376 && resultCode == Activity.RESULT_OK) {
            startDownload()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateUI() {
        runOnUiThread {
            downloadButton.text =
                if (PackDownloadManager.isDownloading(metadata)) {
                    downloadProgress.secondaryProgress = 0
                    resources.getString(R.string.text_downloading)
                } else if (PackLocalManager.isInstalled(metadata)) {
                    downloadProgress.secondaryProgress = 100
                    resources.getString(R.string.text_remove)
                } else {
                    downloadProgress.secondaryProgress = 0
                    if (metadata.NoFile) {
                        resources.getString(R.string.text_goto_download)
                    } else {
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
                }
            if (!PackDownloadManager.isDownloading(metadata)) {
                downloadButton.gravity = Gravity.CENTER
                downloadSpeed.visibility = View.GONE
                downloadProgress.clearAnimation()
                downloadProgress.progress = 0
                val animation = ObjectAnimator.ofInt(downloadProgress, "progress", 0)
                animation.duration = 200
                animation.interpolator = DecelerateInterpolator()
                animation.start()
            } else {
                downloadButton.gravity = Gravity.CENTER_VERTICAL or Gravity.START
                downloadSpeed.visibility = View.VISIBLE
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
                onButtonClick()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}