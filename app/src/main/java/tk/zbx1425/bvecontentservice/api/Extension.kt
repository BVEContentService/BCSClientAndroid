package tk.zbx1425.bvecontentservice.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.widget.ImageView
import androidx.core.content.ContextCompat
import okhttp3.Request
import org.json.JSONObject
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import java.io.InputStream


fun JSONObject.tryString(key: String?): String {
    return if (this.has(key)) this.getString(key) else ""
}

class DownloadImageTask(var bmImage: ImageView) :
    AsyncTask<String?, Void?, Bitmap?>() {

    override fun onPreExecute() {
        bmImage.setImageDrawable(
            ContextCompat.getDrawable(
                ApplicationContext.context,
                R.drawable.landscape_placeholder
            )
        )
    }

    override fun doInBackground(vararg urls: String?): Bitmap? {
        val urldisplay = urls[0] ?: return null
        if (urldisplay == "") {
            return null
        }
        var mIcon11: Bitmap? = null
        try {
            val request: Request = Request.Builder().url(urldisplay).build()
            val inStream: InputStream =
                MetadataManager.client.newCall(request).execute().body()
                    ?.byteStream() ?: return null
            mIcon11 = BitmapFactory.decodeStream(inStream)
        } catch (e: Exception) {
            Log.e("Error", e.message)
            e.printStackTrace()
            return null
        }
        return mIcon11
    }

    override fun onPostExecute(result: Bitmap?) {
        if (result != null) bmImage.setImageBitmap(result)
    }

}