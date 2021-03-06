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

package tk.zbx1425.bvecontentservice.ui.component

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.preference.*
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.io.PackListManager
import tk.zbx1425.bvecontentservice.io.PackLocalManager
import tk.zbx1425.bvecontentservice.io.log.L4jConfig
import tk.zbx1425.bvecontentservice.io.network.ImageLoader
import java.io.File

class SettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        val listUGCSource = findPreference("listUGCSource") as ListPreference
        val popularitySort = findPreference("popSort") as SwitchPreference
        val cacheSizeLimit: EditTextPreference = findPreference("cacheSize") as EditTextPreference
        cacheSizeLimit.setOnPreferenceChangeListener { _: Preference, any: Any ->
            if (any.toString() != "" && any.toString().matches(Regex("\\d*"))) {
                true
            } else {
                Toast.makeText(
                    activity as Context, String.format(
                        resources.getString(
                            R.string.pref_disklru_err
                        ), any.toString()
                    ), Toast.LENGTH_SHORT
                ).show()
                false
            }
        }
        listUGCSource.setOnPreferenceChangeListener { _: Preference, any: Any ->
            val customUGCSource = findPreference("customUGCSource") as EditTextPreference
            customUGCSource.isEnabled = any == "########"
            true
        }
        popularitySort.setOnPreferenceChangeListener { _: Preference, any: Any ->
            PackListManager.populate(any as Boolean)
            true
        }
        updateElements()
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference == null) return super.onPreferenceTreeClick(preference)
        when (preference.key) {
            "fontSize" -> {
                val fontSizePref = findPreference("fontSize") as SeekBarPreference
                fontSizePref.value = 100
            }
            "useIndexServer", "englishName", "useSourceSpider", "allPacks" -> {
                updateElements()
            }
            "clearTemp" -> {
                PackLocalManager.flushCache()
                ImageLoader.initCache()
                File(L4jConfig.logFile).delete()
                Toast.makeText(
                    ApplicationContext.context,
                    R.string.info_clear_temp,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    fun updateElements() {
        val useIndex = findPreference("useIndexServer") as SwitchPreference
        val indexServerTextBox = findPreference("indexServers")
        val useSpiderSwitch = findPreference("useSourceSpider") as SwitchPreference
        val allPacksSwitch = findPreference("allPacks") as SwitchPreference
        val sourceServerTextBox = findPreference("sourceServers")
        val listUGCSource = findPreference("listUGCSource") as ListPreference
        val customUGCSource = findPreference("customUGCSource") as EditTextPreference
        val popularitySort = findPreference("popSort") as SwitchPreference
        indexServerTextBox.isEnabled = useIndex.isChecked
        useSpiderSwitch.isEnabled = useIndex.isChecked
        sourceServerTextBox.isEnabled = !useIndex.isChecked
        popularitySort.isEnabled = !allPacksSwitch.isChecked
        val entries = ArrayList<CharSequence>()
        val entryValues = ArrayList<CharSequence>()
        entries.add(resources.getString(R.string.pref_ugc_disable))
        entryValues.add("$$$$$$$$")
        for (metadata in MetadataManager.ugcServers) {
            entries.add(metadata.Name)
            entryValues.add(metadata.APIURL)
        }
        entries.add(resources.getString(R.string.pref_ugc_custom))
        entryValues.add("########")
        listUGCSource.entries = entries.toArray(arrayOf<CharSequence>())
        listUGCSource.entryValues = entryValues.toArray(arrayOf<CharSequence>())
        if (listUGCSource.value !in entryValues) {
            listUGCSource.setValueIndex(1)
            listUGCSource.value = entryValues[1].toString()
        }
        customUGCSource.isEnabled = listUGCSource.value == "########"
    }

}