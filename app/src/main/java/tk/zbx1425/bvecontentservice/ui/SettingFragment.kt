package tk.zbx1425.bvecontentservice.ui

import android.os.Bundle
import android.widget.Toast
import androidx.preference.*
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.api.MetadataManager
import tk.zbx1425.bvecontentservice.storage.PackLocalManager

class SettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        updateElements()
        val listUGCSource = findPreference("listUGCSource") as ListPreference
        listUGCSource.setOnPreferenceChangeListener { preference: Preference, any: Any ->
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
        val sourceServerTextBox = findPreference("sourceServers")
        val listUGCSource = findPreference("listUGCSource") as ListPreference
        val customUGCSource = findPreference("customUGCSource") as EditTextPreference
        indexServerTextBox.isEnabled = switchBox.isChecked
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
        if (listUGCSource.value == null) {
            listUGCSource.setValueIndex(1)
        }
        customUGCSource.isEnabled = listUGCSource.value == "########"
    }

}