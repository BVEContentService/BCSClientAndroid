package tk.zbx1425.bvecontentservice.io

import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.model.IndexMetadata
import tk.zbx1425.bvecontentservice.getPreference

object UGCSelector {
    fun getActiveUGCServer(): IndexMetadata? {
        val ugcSrc = getPreference("listUGCSource", "********")
        val customUgc = getPreference("customUGCSource", "")
        return when (ugcSrc) {
            "" -> null
            "########" -> IndexMetadata(customUgc)
            "********" -> if (MetadataManager.ugcServers.count() > 0) MetadataManager.ugcServers[0]
            else IndexMetadata(customUgc)
            else -> MetadataManager.ugcServers.find { it.APIURL == ugcSrc }
        }
    }
}