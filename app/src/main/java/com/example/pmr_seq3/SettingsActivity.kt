package com.example.pmr_seq3
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Gérer le bouton "Vider l'historique"
            val clearPrefButton: Preference? = findPreference("clear_history")
            clearPrefButton?.setOnPreferenceClickListener {
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                prefs.edit()
                    .remove("last_pseudo")
                    .remove("last_password")
                    .apply()
                Toast.makeText(context, "Historique vidé", Toast.LENGTH_SHORT).show()
                true
            }

            // Gérer le bouton "Déconnexion"
            val logoutButton: Preference? = findPreference("logout")
            logoutButton?.setOnPreferenceClickListener {
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                prefs.edit()
                    .remove("auth_token")
                    .remove("user_id")
                    .remove("user_pseudo")
                    .apply()
                Toast.makeText(context, "Déconnexion effectuée", Toast.LENGTH_SHORT).show()
                true
            }
        }
    }
} 