package kz.ascoa.embeserver

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import kz.ascoa.embeserver.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}