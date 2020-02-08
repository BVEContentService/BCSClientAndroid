package tk.zbx1425.bvecontentservice.ui

import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import tk.zbx1425.bvecontentservice.ApplicationContext
import tk.zbx1425.bvecontentservice.R
import tk.zbx1425.bvecontentservice.storage.PackLocalManager

class SettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        val switchBox = findPreference("noIndexServer") as SwitchPreference
        val indexServerTextBox = findPreference("indexServers")
        val sourceServerTextBox = findPreference("sourceServers")
        indexServerTextBox.isEnabled = !switchBox.isChecked
        sourceServerTextBox.isEnabled = switchBox.isChecked
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference == null) return super.onPreferenceTreeClick(preference)
        when (preference.key) {
            "noIndexServer" -> {
                val switchBox = findPreference("noIndexServer") as SwitchPreference
                val indexServerTextBox = findPreference("indexServers")
                val sourceServerTextBox = findPreference("sourceServers")
                indexServerTextBox.isEnabled = !switchBox.isChecked
                sourceServerTextBox.isEnabled = switchBox.isChecked
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

}