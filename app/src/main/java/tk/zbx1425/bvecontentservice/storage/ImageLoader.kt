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

package tk.zbx1425.bvecontentservice.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Environment
import android.os.Environment.isExternalStorageRemovable
import android.util.LruCache
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.jakewharton.disklrucache.DiskLruCache
import okhttp3.Request
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata
import tk.zbx1425.bvecontentservice.log.Log
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


object ImageLoader {
    const val LOGCAT_TAG = "BCSImage"

    val lruCache = object : LruCache<String, Bitmap>
        ((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
    val diskLruCache: DiskLruCache = DiskLruCache.open(
        getDiskCacheDir(
            ApplicationContext.context,
            "thumb"
        ), 1, 1,
        (PreferenceManager.getDefaultSharedPreferences(ApplicationContext.context)
            .getString("cacheSize", "10")?.toLongOrNull() ?: 10) * 1024 * 1024
    )

    fun getDiskCacheDir(context: Context, uniqueName: String): File? {
        val cachePath: String =
            if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() ||
                !isExternalStorageRemovable()
            )
                context.externalCacheDir!!.path
            else
                context.cacheDir.path
        Log.i(LOGCAT_TAG, "Disk Cache Dir is " + cachePath + File.separator.toString() + uniqueName)
        return File(cachePath + File.separator.toString() + uniqueName)
    }

    fun getBitmap(url: String, source: SourceMetadata? = null): Bitmap? {
        if (url == "") return null
        val memoryBitmap = lruCache.get(
            hashKeyForDisk(url)
        )
        if (memoryBitmap != null) {
            Log.i(LOGCAT_TAG, "Used Memory Bitmap for " + url)
            return memoryBitmap
        }
        if (!diskLruCache.isClosed) {
            val diskBitmap = diskLruCache.get(
                hashKeyForDisk(
                    url
                )
            )
            if (diskBitmap != null) {
                Log.i(
                    LOGCAT_TAG, "Used Disk Bitmap for " + hashKeyForDisk(
                        url
                    )
                )
                return BitmapFactory.decodeStream(diskBitmap.getInputStream(0))
            }
        }
        var networkBitmap: Bitmap? = null
        try {
            val inStream: InputStream =
                if (source != null) {
                    val request: Request = Request.Builder().url(url).build()
                    HttpHelper.getSourceClient(source).newCall(request).execute().body()
                        ?.byteStream() ?: return null
                } else {
                    val request: Request = Request.Builder().url(url).build()
                    MetadataManager.client.newCall(request).execute().body()
                        ?.byteStream() ?: return null
                }
            networkBitmap = BitmapFactory.decodeStream(inStream) ?: return null
            Log.i(
                LOGCAT_TAG, "Put image in memory: " + hashKeyForDisk(
                    url
                )
            )
            lruCache.put(
                hashKeyForDisk(
                    url
                ), networkBitmap
            )
            val editor = diskLruCache.edit(
                hashKeyForDisk(
                    url
                )
            )
            if (editor != null) {
                val outputStream: OutputStream = editor.newOutputStream(0)
                if (networkBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                    Log.i(
                        LOGCAT_TAG, "Put image in disk: " + hashKeyForDisk(
                            url
                        )
                    )
                    editor.commit()
                } else {
                    editor.abort()
                }
                diskLruCache.flush()
            }
            return networkBitmap
        } catch (e: Exception) {
            Log.e(LOGCAT_TAG, "Fetch Fail", e)
            e.printStackTrace()
            return null
        }
    }

    fun hashKeyForDisk(key: String): String {
        val cacheKey: String
        cacheKey = try {
            val mDigest: MessageDigest = MessageDigest.getInstance("MD5")
            mDigest.update(key.toByteArray())
            bytesToHexString(
                mDigest.digest()
            )
        } catch (e: NoSuchAlgorithmException) {
            key.hashCode().toString()
        }
        return cacheKey
    }

    private fun bytesToHexString(bytes: ByteArray): String { // http://stackoverflow.com/questions/332079
        val sb = StringBuilder()
        for (i in bytes.indices) {
            val hex = Integer.toHexString(0xFF and bytes[i].toInt())
            if (hex.length == 1) {
                sb.append('0')
            }
            sb.append(hex)
        }
        return sb.toString()
    }

    fun isInCache(url: String): Boolean {
        return lruCache.get(
            hashKeyForDisk(url)
        ) != null || diskLruCache.get(
            hashKeyForDisk(url)
        ) != null
    }

    fun setImageAsync(bmImage: ImageView, uri: String) {
        ImageTask(
            bmImage,
            this,
            uri
        ).execute()
    }

    fun setPackImageAsync(bmImage: ImageView, metadata: PackageMetadata) {
        ImageTask(
            bmImage,
            this,
            metadata.Thumbnail,
            metadata.Source
        ).execute()
    }

    fun setPackThumbImageAsync(bmImage: ImageView, metadata: PackageMetadata) {
        if (metadata.ThumbnailLQ_REL != "") {
            ImageTask(
                bmImage,
                this,
                metadata.ThumbnailLQ,
                metadata.Source
            ).execute()
        } else {
            ImageTask(
                bmImage,
                this,
                metadata.Thumbnail,
                metadata.Source
            ).execute()
        }
    }

    class ImageTask(
        var bmImage: ImageView, val loader: ImageLoader,
        val url: String, val source: SourceMetadata? = null
    ) :
        AsyncTask<String?, Void?, Bitmap?>() {

        override fun onPreExecute() {
            /*if (!loader.isInCache(url)) {
                bmImage.setImageDrawable(
                    ContextCompat.getDrawable(
                        ApplicationContext.context,
                        R.drawable.landscape_placeholder
                    )
                )
            }*/
            /*bmImage.setImageDrawable(ColorDrawable(
                ContextCompat.getColor(ApplicationContext.context, R.color.colorPrimaryDark)))*/
            //It has to be reload everytime, since RecyclerView will recycle some elements into new places!
            bmImage.setImageDrawable(
                ContextCompat.getDrawable(
                    ApplicationContext.context,
                    R.drawable.landscape_placeholder
                )
            )
        }

        override fun doInBackground(vararg urls: String?): Bitmap? {
            if (url == "") return null
            return getBitmap(
                url,
                source
            )
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