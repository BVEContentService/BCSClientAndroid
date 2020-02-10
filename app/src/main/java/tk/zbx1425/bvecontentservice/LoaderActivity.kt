package tk.zbx1425.bvecontentservice

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_loader.*
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.storage.PackListManager
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
            if (!PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("useIndexServer", true)
            ) {
                val sourceServers = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString("sourceServers", "https://api.zbx1425.tk:8953/bcs-src")
                    ?.split("\n") ?: ArrayList()
                MetadataManager.fetchMetadataBySource(sourceServers) { message ->
                    runOnUiThread { progress(message) }
                }
            } else {
                val indexServers = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(
                        "indexServers", "https://zbx1425.gitee.io/bcs-index," +
                                "https://zbx1425.github.io/bcs-index"
                    )?.split("\n") ?: ArrayList()
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
        Log.i("BCSFetch", message)
        currentStep.text = message
        adapterData.add(message)
        if ("ERROR" in message) {
            errorCount++
            errorList.add(message)
        }
        adapter.notifyDataSetChanged()
    }
}