package tk.zbx1425.bvecontentservice.ui

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import tk.zbx1425.bvecontentservice.R

class SettingFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        if (preference == null) return super.onPreferenceTreeClick(preference)
        if (preference.key == "noIndexServer") {
            val switchBox = findPreference("noIndexServer") as SwitchPreference
            val indexServerTextBox = findPreference("indexServers")
            val sourceServerTextBox = findPreference("sourceServers")
            indexServerTextBox.isEnabled = !switchBox.isChecked
            sourceServerTextBox.isEnabled = switchBox.isChecked
        }
        return super.onPreferenceTreeClick(preference)
    }

}