package tk.zbx1425.bvecontentservice.ui

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.BuildConfig
import tk.zbx1425.bvecontentservice.MainActivity
import tk.zbx1425.bvecontentservice.R
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

fun showPreviousCrash(activity: Activity){
    val hintFile = File(PackLocalManager.appDir, ".crashed_hint")
    if (!hintFile.exists()) return
    Log.i("BCSExpUtil", "Crash hint is found")
    val lines = hintFile.readLines()
    Log.i("BCSExpUtil", lines.joinToString(","))
    hintFile.delete()
    if (lines.count() < 2) return
    val dlgAlert = AlertDialog.Builder(activity)
    dlgAlert.setCancelable(false)
    dlgAlert.setTitle(R.string.app_name)
    dlgAlert.setMessage(String.format(activity.resources.getString(R.string.bullshit)
        , lines[0], lines[1]))
    dlgAlert.setPositiveButton(android.R.string.yes, null)
    dlgAlert.create().show()
}

fun hThread(r: () -> Unit): Thread{
    val thread = Thread(r)
    bindHandlerToThread(thread)
    return thread
}