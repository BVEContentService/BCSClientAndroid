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
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import tk.zbx1425.bvecontentservice.BuildConfig
import tk.zbx1425.bvecontentservice.MainActivity
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.Version
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.io.PackListManager
import tk.zbx1425.bvecontentservice.io.hThread
import tk.zbx1425.bvecontentservice.io.log.Log
import tk.zbx1425.bvecontentservice.io.network.PackDownloadManager
import java.util.*
import kotlin.system.exitProcess


class LoaderActivity : AppCompatActivity() {

    @Volatile
    var errorCount = 0
    lateinit var adapter: ArrayAdapter<String>
    val adapterData: ArrayList<String> = ArrayList()
    val errorList: ArrayList<String> = ArrayList()

    lateinit var continueButton: Button
    lateinit var stepLog: ListView
    lateinit var progressBar: ProgressBar
    lateinit var currentStep: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (getPreference("showLoadLog", false)) {
            setContentView(R.layout.activity_loader)
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.activity_loader_production)
            supportActionBar?.hide()
        }
        continueButton = findViewById(R.id.continueButton)
        stepLog = findViewById(R.id.stepLog)
        progressBar = findViewById(R.id.progressBar)
        currentStep = findViewById(R.id.currentStep)
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
        val fetchThread = hThread {
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
                    stepLog.visibility = View.VISIBLE
                }
                if (MetadataManager.updateMetadata != null && MetadataManager.updateMetadata!!.Version > Version(BuildConfig.VERSION_NAME)) {
                    dlgAlert.setMessage(
                        String.format(
                            resources.getString(R.string.info_update_app),
                            MetadataManager.updateMetadata!!.Version.get(), MetadataManager.updateMetadata!!.Description
                        )
                    )
                    dlgAlert.setCancelable(false)
                    dlgAlert.setPositiveButton(android.R.string.yes) { _: DialogInterface, _: Int ->
                        adapterData.add("")
                        adapterData.add(resources.getString(R.string.info_update_start))
                        currentStep.text = resources.getString(R.string.info_update_start)
                        progressBar.visibility = View.VISIBLE
                        adapter.notifyDataSetChanged()
                        PackDownloadManager.startSelfUpdateDownload(MetadataManager.updateMetadata!!.File) {
                            runOnUiThread {
                                adapterData.add(it)
                                currentStep.text = it
                                adapter.notifyDataSetChanged()
                                continueButton.visibility = View.VISIBLE
                                progressBar.visibility = View.GONE
                                stepLog.visibility = View.VISIBLE
                            }
                        }
                        continueButton.visibility = View.GONE
                    }
                    dlgAlert.setNegativeButton(android.R.string.no) { _: DialogInterface, _: Int ->
                        if (MetadataManager.updateMetadata?.Force == true) {
                            exitProcess(0)
                        } else if (errorCount == 0) {
                            callMainActivity()
                        }
                    }
                    if (!isFinishing) {
                        dlgAlert.create().show()
                    }
                } else if (updateCnt > 0) {
                    dlgAlert.setMessage(String.format(resources.getString(R.string.info_update_pack), updateCnt))
                    dlgAlert.setCancelable(false)
                    dlgAlert.setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                        if (errorCount == 0) {
                            callMainActivity()
                        }
                    }
                    if (!isFinishing) {
                        dlgAlert.create().show()
                    }
                } else if (errorCount == 0) {
                    if (getPreference("showLoadLog", false)) {
                        continueButton.visibility = View.VISIBLE
                    } else {
                        callMainActivity()
                    }
                }
            }
        }
        MetadataManager.cleanUp()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permSucceed = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (!permSucceed) {
                progressBar.visibility = View.GONE
                adapterData.add(resources.getString(R.string.permission_restart))
                adapter.notifyDataSetChanged()
                currentStep.text = resources.getString(R.string.permission_restart)
                progressBar.visibility = View.GONE
                continueButton.visibility = View.GONE
                return
            }
        }
        if (!isInternetAvailable(this)) {
            MetadataManager.initialized = true
            PackListManager.populate()
            progressBar.visibility = View.GONE
            currentStep.text = resources.getString(R.string.info_no_network)
            val dlgAlert = AlertDialog.Builder(this)
            dlgAlert.setCancelable(false)
            dlgAlert.setTitle(R.string.app_name)
            dlgAlert.setMessage(R.string.info_no_network)
            dlgAlert.setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                callMainActivity()
            }
            dlgAlert.create().show()
            return
        }
        fetchThread.start()
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
            dlgAlert.setCancelable(false)
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
                dlgAlert.setCancelable(false)
                dlgAlert.setTitle(R.string.app_name)
                dlgAlert.setMessage(R.string.permission_fail)
                dlgAlert.setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                    finishAffinity()
                }
                if (!isFinishing) {
                    dlgAlert.create().show()
                }
                Log.e("BCSBullshit", "You fucking stubborn bastard!")
                exitProcess(0)
            }
        }
    }

    private fun isInternetAvailable(context: Context): Boolean {
        var result = false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            connectivityManager.run {
                connectivityManager.activeNetworkInfo?.run {
                    result = type != ConnectivityManager.TYPE_DUMMY
                }
            }
        }
        return result
    }
}