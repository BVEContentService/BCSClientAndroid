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

import Identification
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_about.*
import tk.zbx1425.bvecontentservice.BuildConfig
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.log.L4jConfig
import tk.zbx1425.bvecontentservice.replaceView
import tk.zbx1425.bvecontentservice.ui.component.MetadataView
import tk.zbx1425.bvecontentservice.ui.sendReport
import java.io.*
import java.util.*


class AboutActivity : AppCompatActivity() {

    private var clickTimes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        appMetadataPlaceholder.replaceView(MetadataView(this))
        if (getPreference("showLoadLog", false)){
            iconLayout.setOnClickListener {
                clickTimes++
                if (clickTimes >= 25) aboutHidden.visibility = View.VISIBLE
                //throw RuntimeException("This should never happen")
            }
        }
        buttonSubmitLogcat.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.text_log_settag)
            builder.setMessage(R.string.text_log_warning)
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)
            builder.setPositiveButton(android.R.string.yes) { intf: DialogInterface, _: Int ->
                intf.dismiss()
                if (input.text.isNotEmpty()) {
                    val outputWriter = StringWriter()
                    val writer = PrintWriter(outputWriter)
                    writer.println("BCS LogCat Dump")
                    writer.println("Tag: " + input.text)
                    writer.println("UUID: " + Identification.deviceID)
                    writer.println("IPV4: " + Identification.IPAddress)
                    writer.println(
                        BuildConfig.VERSION_NAME + " " +
                                BuildConfig.BUILD_TYPE + " " + BuildConfig.BUILD_TIME
                    )
                    writer.println("Triggered " + Date().toString())
                    writer.println()
                    val process = Runtime.getRuntime().exec("logcat -d")
                    val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                    val buffer = CharArray(1024)
                    var len: Int
                    while (bufferedReader.read(buffer).also { len = it } >= 0) {
                        writer.write(buffer, 0, len)
                    }
                    writer.flush()
                    if (input.text.startsWith("full")) {
                        writer.write("\nSaved BCS Log:\n")
                        writer.write(File(L4jConfig.logFile).readText())
                    }
                    val metadata = MetadataManager.updateMetadata
                    if (metadata != null && metadata.CrashReport_REL != "") {
                        sendReport(outputWriter.toString(), "logcat")
                        Toast.makeText(this, R.string.info_log_sent, Toast.LENGTH_SHORT).show()
                    } else {
                        val showText = TextView(this)
                        showText.text = outputWriter.toString()
                        showText.setTextIsSelectable(true)
                        val copyableTextDlg = AlertDialog.Builder(this)
                        copyableTextDlg.setView(showText).setCancelable(true).show()
                    }
                }
            }
            builder.setNegativeButton(android.R.string.no, null)
            builder.show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}