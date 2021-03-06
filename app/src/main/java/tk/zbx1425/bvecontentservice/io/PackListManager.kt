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

package tk.zbx1425.bvecontentservice.io

import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.api.Version
import tk.zbx1425.bvecontentservice.api.model.PackageMetadata
import tk.zbx1425.bvecontentservice.getPreference
import tk.zbx1425.bvecontentservice.io.PackLocalManager.decodeInvisibleString
import tk.zbx1425.bvecontentservice.io.log.Log
import tk.zbx1425.bvecontentservice.io.network.UGCManager.getPopSequence
import tk.zbx1425.bvecontentservice.ui.SectionsPagerAdapter

object PackListManager {
    const val LOGCAT_TAG = "BCSPackListMan"
    val localList: ArrayList<PackageMetadata> = ArrayList()
    var onlineList: ArrayList<PackageMetadata> = ArrayList()
    lateinit var pagerAdapter: SectionsPagerAdapter

    fun stripExtension(str: String): String { // Handle null case specially.
        val pos = str.lastIndexOf(".")
        return if (pos == -1) str else str.substring(0, pos)
    }

    fun populate(popSort: Boolean? = null): Int {
        var updateCount = 0
        localList.clear(); onlineList.clear()
        val localPacks = HashMap(PackLocalManager.getLocalPacks().map {
            val parts = it.nameWithoutExtension.split(PackLocalManager.BCS_DELIMITER)
            if (parts.count() > 3) {
                parts[2] to Version(decodeInvisibleString(parts[1]))
            } else {
                it.nameWithoutExtension to Version("0.0")
            }
        }.toMap())
        for (pack in MetadataManager.packMap) {
            if (localPacks.containsKey(pack.key)) {
                Log.i(
                    LOGCAT_TAG, "Pack " + pack.key + " found on local disk with ver " +
                            localPacks[pack.key]?.get()
                )
                if (localPacks[pack.key]!! < pack.value.Version) {
                    Log.i(LOGCAT_TAG, "Pack " + pack.key + " can be updated")
                    pack.value.UpdateAvailable = true
                    updateCount++
                } else {
                    pack.value.UpdateAvailable = false
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
            localList.add(PackageMetadata(stripExtension(pack.key)))
            //localList.add(PackageMetadata(pack.key))
        }
        if (getPreference("allPacks", false)) {
            onlineList = MetadataManager.packs
        }
        if (!(popSort ?: getPreference("popSort", false))) {
            Log.i(LOGCAT_TAG, "Sorted by Timestamp")
            onlineList.sortByDescending { it.Timestamp }
        } else {
            Log.i(LOGCAT_TAG, "Sorted by PopSequence")
            val popSequence = getPopSequence()
            Log.i(LOGCAT_TAG, popSequence.joinToString(","))
            onlineList.sortWith(compareByDescending<PackageMetadata> { popSequence.indexOf(it.ID) }
                .thenByDescending { it.Timestamp })
        }
        localList.sortWith(compareBy({ it.DummyPack }, { !it.UpdateAvailable }))
        if (::pagerAdapter.isInitialized) {
            pagerAdapter.notifyAllAdapters()
        }
        return updateCount
    }
}