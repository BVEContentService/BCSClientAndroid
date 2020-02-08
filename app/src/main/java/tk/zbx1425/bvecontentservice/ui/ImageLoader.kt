package tk.zbx1425.bvecontentservice.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import androidx.core.content.ContextCompat
import okhttp3.Request
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import java.io.InputStream

object ImageLoader {
    val lruCache = object : LruCache<String, Bitmap>
        ((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun getBitmap(url: String): Bitmap? {
        if (url == "") return null
        val memoryBitmap = lruCache.get(url)
        if (memoryBitmap != null) return memoryBitmap
        var networkBitmap: Bitmap? = null
        try {
            val request: Request = Request.Builder().url(url).build()
            val inStream: InputStream =
                MetadataManager.client.newCall(request).execute().body()
                    ?.byteStream() ?: return null
            networkBitmap = BitmapFactory.decodeStream(inStream) ?: return null
            lruCache.put(url, networkBitmap)
            return networkBitmap
        } catch (e: Exception) {
            Log.e("Error", e.message ?: "")
            e.printStackTrace()
            return null
        }
    }

    fun getPackThumbnail(source: PackageMetadata): Bitmap? {
        if (source.Thumbnail == "") return null
        val memoryBitmap = lruCache.get(source.Thumbnail)
        if (memoryBitmap != null) return memoryBitmap
        var networkBitmap: Bitmap? = null
        try {
            val request: Request = Request.Builder().url(source.Thumbnail).build()
            val inStream: InputStream =
                HttpHelper.getSourceClient(source.Source).newCall(request).execute().body()
                    ?.byteStream() ?: return null
            networkBitmap = BitmapFactory.decodeStream(inStream) ?: return null
            lruCache.put(source.Thumbnail, networkBitmap)
            return networkBitmap
        } catch (e: Exception) {
            Log.e("Error", e.message ?: "")
            e.printStackTrace()
            return null
        }
    }

    fun isInCache(url: String): Boolean {
        return lruCache.get(url) != null
    }

    fun setImageAsync(bmImage: ImageView, uri: String) {
        ImageTask(bmImage, this, uri).execute()
    }

    fun setPackImageAsync(bmImage: ImageView, metadata: PackageMetadata) {
        PackImageTask(bmImage, this, metadata).execute()
    }

    class ImageTask(var bmImage: ImageView, val loader: ImageLoader, val url: String) :
        AsyncTask<String?, Void?, Bitmap?>() {

        override fun onPreExecute() {
            if (!loader.isInCache(url)) {
                bmImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        ApplicationContext.context,
                        R.drawable.landscape_placeholder
                    )
                )
            }
        }

        override fun doInBackground(vararg urls: String?): Bitmap? {
            if (url == "") return null
            return loader.getBitmap(url)
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                bmImage.setImageBitmap(result)
            } else {
                bmImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        ApplicationContext.context,
                        R.drawable.landscape_placeholder
                    )
                )
            }
        }
    }

    class PackImageTask(
        var bmImage: ImageView,
        val loader: ImageLoader,
        val metadata: PackageMetadata
    ) :
        AsyncTask<String?, Void?, Bitmap?>() {

        override fun onPreExecute() {
            if (!loader.isInCache(metadata.Thumbnail)) {
                bmImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        ApplicationContext.context,
                        R.drawable.landscape_placeholder
                    )
                )
            }
        }

        override fun doInBackground(vararg urls: String?): Bitmap? {
            if (metadata.Thumbnail == "") return null
            return loader.getPackThumbnail(metadata)
        }

        override fun onPostExecute(result: Bitmap?) {
            if (result != null) {
                bmImage.setImageBitmap(result)
            } else {
                bmImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        ApplicationContext.context,
                        R.drawable.landscape_placeholder
                    )
                )
            }
        }
    }
}