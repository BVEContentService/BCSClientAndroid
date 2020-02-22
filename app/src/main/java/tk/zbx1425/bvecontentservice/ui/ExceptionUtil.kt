package tk.zbx1425.bvecontentservice.ui

import Identification
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.BuildConfig
import tk.zbx1425.bvecontentservice.MainActivity
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.io.PackLocalManager
import tk.zbx1425.bvecontentservice.log.Log
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess


fun bindHandlerToThread(thread: Thread){
    val hintFile = File(PackLocalManager.appDir, ".crashed_hint")
    thread.uncaughtExceptionHandler = object: Thread.UncaughtExceptionHandler{
        override fun uncaughtException(t: Thread, e: Throwable) {
            val dumpFile = File(PackLocalManager.appDir, "crash_"+
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".txt")
            val writer = PrintWriter(dumpFile)
            writer.println("BCS Exception Trace")
            writer.println("UUID: " + Identification.deviceID)
            writer.println(BuildConfig.VERSION_NAME + " " +
                    BuildConfig.BUILD_TYPE + " " + BuildConfig.BUILD_TIME)
            writer.println("Triggered " + Date().toString())
            writer.println(e.message)
            writer.println()
            e.printStackTrace(writer)
            writer.flush()
            writer.close()
            hintFile.writeText(String.format("%s\n%s", e.message, dumpFile.absolutePath))

            val intent = Intent(ApplicationContext.context, MainActivity::class.java)
            intent.putExtra("crash", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    or Intent.FLAG_ACTIVITY_NEW_TASK)
            val pendingIntent =
                PendingIntent.getActivity(ApplicationContext.context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            val mgr = ApplicationContext.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr[AlarmManager.RTC, System.currentTimeMillis() + 100] = pendingIntent
            exitProcess(-2)
        }
    }
}

fun showPreviousCrash() {
    val hintFile = File(PackLocalManager.appDir, ".crashed_hint")
    if (!hintFile.exists()) return
    Log.i("BCSExpUtil", "Crash hint is found")
    val lines = hintFile.readLines()
    Log.i("BCSExpUtil", lines.joinToString(","))
    hintFile.delete()
    if (lines.count() < 2) return
    val metadata = MetadataManager.updateMetadata
    val message: String = if (metadata != null && metadata.CrashReport_REL != "") {
        sendReport(File(lines[1]).readText(), "text")
        String.format(ApplicationContext.context.resources.getString(R.string.bullshit_eaten), lines[0])
    } else {
        String.format(ApplicationContext.context.resources.getString(R.string.bullshit), lines[0], lines[1])
    }
    Toast.makeText(ApplicationContext.context, message, Toast.LENGTH_LONG).show()
}

fun sendReport(text: String, type: String) {
    val metadata = MetadataManager.updateMetadata
    if (metadata != null && metadata.CrashReport_REL != "") {
        Thread {
            val body = RequestBody.create(MediaType.parse("text/plain"), text)
            try {
                val builder = Request.Builder().url(
                    String.format(
                        metadata.CrashReport,
                        type, "and" + BuildConfig.VERSION_NAME, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    )
                )
                    .addHeader("X-BCS-UUID", Identification.deviceID)
                    .addHeader("X-BCS-CHECKSUM", Identification.getDateChecksum())
                val request = when (metadata.ReportMethod) {
                    "DAV" -> builder.put(body).build()
                    else -> builder.post(body).build()
                }
                HttpHelper.client.newCall(request).execute()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }.start()
    }
}

fun hThread(r: () -> Unit): Thread{
    val thread = Thread(r)
    bindHandlerToThread(thread)
    return thread
}