package tk.zbx1425.bvecontentservice.storage

import android.util.Log
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.PackageMetadata
import tk.zbx1425.bvecontentservice.api.Version

object PackListManager {
    const val LOGCAT_TAG = "BCSPackListMan"
    val localList: ArrayList<PackageMetadata> = ArrayList()
    var onlineList: ArrayList<PackageMetadata> = ArrayList()

    fun stripExtension(str: String): String { // Handle null case specially.
        val pos = str.lastIndexOf(".")
        return if (pos == -1) str else str.substring(0, pos)
    }

    fun populate() {
        localList.clear(); onlineList.clear()
        val localPacks = HashMap(PackLocalManager.getLocalPacks().map {
            val parts = stripExtension(it.nameWithoutExtension).split("_")
            if (parts.count() > 1) {
                parts[0] to Version(parts[1])
            } else {
                it.nameWithoutExtension to Version("0.0")
            }
        }.toMap())
        for (pack in MetadataManager.packMap) {
            if (localPacks.containsKey(pack.key)) {
                Log.i(LOGCAT_TAG, "Pack " + pack.key + " found on local disk")
                if (localPacks[pack.key]!! < pack.value.Version) {
                    Log.i(LOGCAT_TAG, "Pack " + pack.key + " can be updated")
                }
                localPacks.remove(pack.key)
                localList.add(pack.value)
            } else {
                Log.i(LOGCAT_TAG, "Pack " + pack.key + " not installed")
                onlineList.add(pack.value)
            }
        }
        for (pack in localPacks) {
            Log.i(LOGCAT_TAG, "Non-indexed pack " + pack.key)
            //localList.add(PackageMetadata(pack.key))
        }
    }
}