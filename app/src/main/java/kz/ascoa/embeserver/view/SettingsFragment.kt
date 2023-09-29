package kz.ascoa.embeserver.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import kz.ascoa.embeserver.R
import kz.ascoa.embeserver.showAlert

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var modelPreference: Preference? = findPreference<Preference>("device_model")
        modelPreference?.setOnPreferenceChangeListener { preference, newValue ->

            var permissionArr = arrayOf<String>(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            if(newValue == "pref_model_zebra_value"){
                val statePermission =
                    activity?.let { ActivityCompat.checkSelfPermission(it, Manifest.permission.BLUETOOTH_CONNECT) }
                if (statePermission != PackageManager.PERMISSION_GRANTED) {
                    activity?.let {
                        ActivityCompat.requestPermissions(
                            it,
                            permissionArr,
                            1
                        )
                    }
                }
                // activity?.let { showAlert(it, "From reader change") }
            }
            true
        }



        return super.onCreateView(inflater, container, savedInstanceState)
    }


}