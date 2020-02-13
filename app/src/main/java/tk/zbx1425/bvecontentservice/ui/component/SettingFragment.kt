package tk.zbx1425.bvecontentservice.ui.component

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.preference.*
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.storage.ImageLoader
import tk.zbx1425.bvecontentservice.storage.PackLocalManager

class SettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        updateElements()
        val listUGCSource = findPreference("listUGCSource") as ListPreference
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
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference == null) return super.onPreferenceTreeClick(preference)
        when (preference.key) {
            "useIndexServer", "englishName" -> {
                updateElements()
            }
            "clearTemp" -> {
                PackLocalManager.flushCache()
                ImageLoader.lruCache.evictAll()
                ImageLoader.diskLruCache.delete()
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
        val switchBox = findPreference("useIndexServer") as SwitchPreference
        val indexServerTextBox = findPreference("indexServers")
        val useSpiderSwitch = findPreference("useSourceSpider")
        val sourceServerTextBox = findPreference("sourceServers")
        val listUGCSource = findPreference("listUGCSource") as ListPreference
        val customUGCSource = findPreference("customUGCSource") as EditTextPreference
        indexServerTextBox.isEnabled = switchBox.isChecked
        useSpiderSwitch.isEnabled = switchBox.isChecked
        sourceServerTextBox.isEnabled = !switchBox.isChecked
        val entries = ArrayList<CharSequence>()
        val entryValues = ArrayList<CharSequence>()
        entries.add(resources.getString(R.string.pref_ugc_disable))
        entryValues.add("")
        for (metadata in MetadataManager.ugcServers) {
            entries.add(metadata.Name)
            entryValues.add(metadata.APIURL)
        }
        entries.add(resources.getString(R.string.pref_ugc_custom))
        entryValues.add("########")
        listUGCSource.entries = entries.toArray(arrayOf<CharSequence>())
        listUGCSource.entryValues = entryValues.toArray(arrayOf<CharSequence>())
        if (listUGCSource.value == null || listUGCSource.value == "") {
            listUGCSource.setValueIndex(1)
        }
        customUGCSource.isEnabled = listUGCSource.value == "########"
    }

}