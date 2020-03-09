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

package tk.zbx1425.bvecontentservice.io.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Environment
import android.os.Environment.isExternalStorageRemovable
import android.util.LruCache
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.jakewharton.disklrucache.DiskLruCache
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.api.model.SourceMetadata
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.io.log.Log
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


object ImageLoader {
    const val LOGCAT_TAG = "BCSImage"

    lateinit var lruCache: LruCache<String, Bitmap>
    lateinit var diskLruCache: DiskLruCache

    fun initCache() {
        if (ImageLoader::lruCache.isInitialized) lruCache.evictAll()
        if (ImageLoader::diskLruCache.isInitialized) diskLruCache.delete()
        lruCache = object : LruCache<String, Bitmap>
            ((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
        diskLruCache = DiskLruCache.open(
            getDiskCacheDir(ApplicationContext.context, "thumb"), 1, 1,
            (getPreference("cacheSize", "50").toLongOrNull() ?: 50) * 1024 * 1024
        )
    }

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

    fun getBitmap(url: String, source: SourceMetadata? = null, writeToDisk: Boolean = true): Bitmap? {
        if (url == "") return null
        val memoryBitmap = lruCache.get(
            hashKeyForDisk(url)
        )
        if (memoryBitmap != null) {
            Log.i(LOGCAT_TAG, "Used Memory Bitmap for " + url)
            return memoryBitmap
        }
        if (!diskLruCache.isClosed) {
            val diskBitmap = diskLruCache.get(hashKeyForDisk(url))
            if (diskBitmap != null) {
                Log.i(LOGCAT_TAG, "Used Disk Bitmap for " + url)
                return BitmapFactory.decodeStream(diskBitmap.getInputStream(0))
            }
        }
        var networkBitmap: Bitmap? = null
        try {
            val inStream: InputStream = HttpHelper.openStream(source, url) ?: return null
            networkBitmap = BitmapFactory.decodeStream(inStream) ?: return null
            Log.i(LOGCAT_TAG, "Put image in memory: " + url)
            lruCache.put(hashKeyForDisk(url), networkBitmap)
            if (writeToDisk) {
                val editor = diskLruCache.edit(hashKeyForDisk(url))
                if (editor != null) {
                    val outputStream: OutputStream = editor.newOutputStream(0)
                    if (networkBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        Log.i(LOGCAT_TAG, "Put image in disk: " + url)
                        editor.commit()
                    } else {
                        editor.abort()
                    }
                    diskLruCache.flush()
                }
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

    fun setImageAsync(bmImage: ImageView, uri: String, writeToDisk: Boolean = true) {
        ImageTask(bmImage, this, uri, null, writeToDisk).execute()
    }

    fun setPackImageAsync(bmImage: ImageView, metadata: PackageMetadata, writeToDisk: Boolean = true) {
        ImageTask(bmImage, this, metadata.Thumbnail, metadata.Source, writeToDisk).execute()
    }

    fun setPackThumbImageAsync(bmImage: ImageView, metadata: PackageMetadata, writeToDisk: Boolean = true) {
        if (metadata.ThumbnailLQ_REL != "") {
            ImageTask(bmImage, this, metadata.ThumbnailLQ, metadata.Source, writeToDisk).execute()
        } else {
            ImageTask(bmImage, this, metadata.Thumbnail, metadata.Source, writeToDisk).execute()
        }
    }

    class ImageTask(
        var bmImage: ImageView, val loader: ImageLoader,
        val url: String, val source: SourceMetadata? = null, val writeToDisk: Boolean = true
    ) :
        AsyncTask<String?, Void?, Bitmap?>() {

        override fun onPreExecute() {
            Log.i(LOGCAT_TAG, "Started fetching " + url)
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
                Log.i(LOGCAT_TAG, "Succeed fetching " + url)
                bmImage.setImageBitmap(result)
            } else {
                Log.i(LOGCAT_TAG, "Gave up fetching " + url)
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