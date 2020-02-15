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

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_loader.*
import tk.zbx1425.bvecontentservice.MainActivity
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.io.PackListManager
import tk.zbx1425.bvecontentservice.log.Log
import java.util.*


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
        Thread {
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
                if (updateCnt > 0) {
                    val dlgAlert = AlertDialog.Builder(this)
                    dlgAlert.setCancelable(true)
                    dlgAlert.setTitle(R.string.app_name)
                    dlgAlert.setMessage(
                        String.format(
                            resources.getString(R.string.info_update_avail),
                            updateCnt
                        )
                    )
                    dlgAlert.setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                        if (errorCount == 0) {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                        }
                    }
                    dlgAlert.create().show()
                }
                if (errorCount > 0) {
                    adapterData.add("")
                    adapterData.add("Fetching finished with " + errorList.count() + " Errors:")
                    adapterData.addAll(errorList)
                    currentStep.text = String.format(
                        resources.getString(R.string.fetch_error_message),
                        errorList.count()
                    )
                    adapter.notifyDataSetChanged()
                    currentStep.setTextColor(Color.RED)
                    continueButton.visibility = View.VISIBLE
                } else if (updateCnt == 0) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }
            }
        }.start()
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
}