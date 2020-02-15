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

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_loader.*
import tk.zbx1425.bvecontentservice.BuildConfig
import tk.zbx1425.bvecontentservice.MainActivity
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.Version
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.io.PackDownloadManager
import tk.zbx1425.bvecontentservice.io.PackListManager
import tk.zbx1425.bvecontentservice.log.Log
import java.util.*
import kotlin.system.exitProcess


class LoaderActivity : AppCompatActivity() {

    @Volatile
    var errorCount = 0
    lateinit var adapter: ArrayAdapter<String>
    val adapterData: ArrayList<String> = ArrayList()
    val errorList: ArrayList<String> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loader)
        continueButton.visibility = View.GONE
        adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1, adapterData
        )
        stepLog.adapter = adapter
        continueButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
        val fetchThread = Thread {
            if (!getPreference("useIndexServer", true)) {
                val sourceServers =
                    getPreference("sourceServers", resources.getString(R.string.default_src))
                        .split("\n")
                MetadataManager.fetchMetadataBySource(sourceServers) { message ->
                    runOnUiThread { progress(message) }
                }
            } else {
                val indexServers = getPreference(
                        "indexServers", resources.getString(R.string.default_index)
                ).split("\n")
                MetadataManager.fetchMetadata(indexServers) { message ->
                    runOnUiThread { progress(message) }
                }
            }
            runOnUiThread { progress("Populating PackListManager...") }
            val updateCnt = PackListManager.populate()
            runOnUiThread {
                progress("Pack List is ready. Thank you!")
                progressBar.visibility = View.GONE
                val dlgAlert = AlertDialog.Builder(this)
                dlgAlert.setTitle(R.string.app_name)
                if (errorCount > 0) {
                    adapterData.add("")
                    adapterData.add("Encountered " + errorList.count() + " Errors:")
                    adapterData.addAll(errorList)
                    currentStep.text = String.format(
                        resources.getString(R.string.fetch_error_message),
                        errorList.count()
                    )
                    adapter.notifyDataSetChanged()
                    currentStep.setTextColor(Color.RED)
                    continueButton.visibility = View.VISIBLE
                }
                if (MetadataManager.updateMetadata != null && MetadataManager.updateMetadata!!.Version > Version(BuildConfig.VERSION_NAME)) {
                    dlgAlert.setMessage(
                        String.format(
                            resources.getString(R.string.info_update_app),
                            MetadataManager.updateMetadata!!.Version.get(), MetadataManager.updateMetadata!!.Description
                        )
                    )
                    dlgAlert.setCancelable(MetadataManager.updateMetadata?.Force == false)
                    dlgAlert.setPositiveButton(android.R.string.yes) { _: DialogInterface, _: Int ->
                        adapterData.add("")
                        adapterData.add(resources.getString(R.string.info_update_start))
                        currentStep.text = resources.getString(R.string.info_update_start)
                        adapter.notifyDataSetChanged()
                        PackDownloadManager.startSelfUpdateDownload(MetadataManager.updateMetadata!!.File)
                        continueButton.visibility = View.GONE
                    }
                    dlgAlert.setNegativeButton(android.R.string.no) { _: DialogInterface, _: Int ->
                        if (MetadataManager.updateMetadata?.Force == true) {
                            exitProcess(0)
                        } else if (errorCount == 0) {
                            callMainActivity()
                        }
                    }
                    dlgAlert.create().show()
                } else if (updateCnt > 0) {
                    dlgAlert.setMessage(String.format(resources.getString(R.string.info_update_pack), updateCnt))
                    dlgAlert.setCancelable(true)
                    dlgAlert.setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                        if (errorCount == 0) {
                            callMainActivity()
                        }
                    }
                    dlgAlert.create().show()
                } else if (errorCount == 0) {
                    callMainActivity()
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                adapterData.add(resources.getString(R.string.permission_restart))
                adapter.notifyDataSetChanged()
                currentStep.text = resources.getString(R.string.permission_restart)
                progressBar.visibility = View.GONE
                continueButton.visibility = View.GONE
            } else {
                fetchThread.start()
            }
        }
    }

    fun callMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
    }

    fun progress(message: String) {
        currentStep.text = message
        adapterData.add(message)
        if ("ERROR" in message) {
            errorCount++
            errorList.add(message)
            Log.e("BCSFetch", message)
        } else {
            Log.i("BCSFetch", message)
        }
        adapter.notifyDataSetChanged()
    }


    private fun checkPermission(perm: String): Boolean {
        val result = ContextCompat.checkSelfPermission(this, perm)
        if (result != PackageManager.PERMISSION_GRANTED) {
            requestPermission(perm)
            return false
        }
        return true
    }

    private fun requestPermission(perm: String) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
            val dlgAlert = AlertDialog.Builder(this)
            dlgAlert.setTitle(R.string.app_name)
            dlgAlert.setMessage(R.string.permission_fail)
            dlgAlert.setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                finishAffinity()
            }
            dlgAlert.create().show()
            Log.e("BCSBullshit", "You fucking careless bastard!")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(perm), 810)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == 810) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                val dlgAlert = AlertDialog.Builder(this)
                dlgAlert.setTitle(R.string.app_name)
                dlgAlert.setMessage(R.string.permission_fail)
                dlgAlert.setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                    finishAffinity()
                }
                dlgAlert.create().show()
                Log.e("BCSBullshit", "You fucking stubborn bastard!")
                exitProcess(0)
            }
        }
    }
}