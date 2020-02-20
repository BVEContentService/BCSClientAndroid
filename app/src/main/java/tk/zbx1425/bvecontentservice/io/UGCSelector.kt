package tk.zbx1425.bvecontentservice.io

import Identification
import okhttp3.Request
import tk.zbx1425.bvecontentservice.api.HttpHelper
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.model.IndexMetadata
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.log.Log
import tk.zbx1425.bvecontentservice.ui.hThread

object UGCSelector {
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
                "%s?pkg=%s&ver=%s&author=%s",
                server.APIURL, metadata.ID, metadata.Version.get(), metadata.Author.ID
            )
        } else {
            String.format(
                "%s?pkg=%s&ver=%s&author=%s&act=%s",
                server.APIURL, metadata.ID, metadata.Version.get(), metadata.Author.ID, action
            )
        }
    }

    fun runActionAsync(metadata: PackageMetadata, action: String) {
        val url = getURL(metadata, action) ?: return
        val devID = Identification.deviceID
        val checkSum = Identification.getChecksum(metadata)
        hThread {
            val request = Request.Builder().url(url)
                .addHeader("X-BCS-UUID", devID)
                .addHeader("X-BCS-CHECKSUM", checkSum)
                .build()
            val response = HttpHelper.client.newCall(request).execute()
            val result = response.body()?.string()
            Log.i("BCSUGC", result ?: "NOINFO")
        }.start()
    }

}