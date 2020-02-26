package tk.zbx1425.bvecontentservice.io.network

import Identification
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.model.IndexMetadata
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.io.hThread
import tk.zbx1425.bvecontentservice.io.log.Log
import java.net.URLEncoder

object UGCManager {
    fun getActiveUGCServer(): IndexMetadata? {
        val ugcSrc = getPreference("listUGCSource", "********")
        val customUgc = getPreference("customUGCSource", "")
        return when (ugcSrc) {
            "$$$$$$$$" -> null
            "########" -> IndexMetadata(customUgc)
            "********", "" -> if (MetadataManager.ugcServers.count() > 0) MetadataManager.ugcServers[0]
            else IndexMetadata(customUgc)
            else -> MetadataManager.ugcServers.find { it.APIURL == ugcSrc }
        }
    }

    fun getURL(metadata: PackageMetadata, action: String = ""): String? {
        val server = getActiveUGCServer() ?: return null
        return if (action == "") {
            String.format(
                "%s?pkg=%s&ver=%s&author=%s", server.APIURL,
                URLEncoder.encode(metadata.ID, "UTF-8"),
                URLEncoder.encode(metadata.Version.get(), "UTF-8"),
                URLEncoder.encode(metadata.Author.ID, "UTF-8")
            )
        } else {
            String.format(
                "%s?pkg=%s&ver=%s&author=%s&act=%s", server.APIURL,
                URLEncoder.encode(metadata.ID, "UTF-8"),
                URLEncoder.encode(metadata.Version.get(), "UTF-8"),
                URLEncoder.encode(metadata.Author.ID, "UTF-8"),
                URLEncoder.encode(action, "UTF-8")
            )
        }
    }

    fun runActionAsync(metadata: PackageMetadata, action: String) {
        val url = getURL(metadata, action) ?: return
        val devID = Identification.deviceID
        val checkSum = Identification.getChecksum(metadata)
        hThread {
            try {
                val request = HttpHelper.getBasicBuilder(url)
                    .header("X-BCS-UUID", devID)
                    .header("X-BCS-CHECKSUM", checkSum)
                    .build()
                val response = HttpHelper.client.newCall(request).execute()
                val result = response.body()?.string()
                Log.i("BCSUGC", result ?: "NOINFO")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }.start()
    }

    fun getPopSequence(): Array<String> {
        try {
            val server = getActiveUGCServer() ?: return arrayOf()
            val array = HttpHelper.fetchArray(server.APIURL + "?act=popseq") ?: return arrayOf()
            return Array(array.length()) { array.getString(it) }
        } catch (ex: Exception) {
            return arrayOf()
        }
    }

}