package cz.dzubera.callwarden.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.utils.PreferencesUtils

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        supportActionBar?.title = "Nastavení";

        val switchAutorestart = findViewById<SwitchCompat>(R.id.auto_restart_value)
        switchAutorestart.isChecked = PreferencesUtils.loadAutoRestartValue(this)
        switchAutorestart.setOnCheckedChangeListener { _, p1 ->
            PreferencesUtils.saveAutoRestartValue(
                this@SettingsActivity,
                p1
            )
        }

        val backButton = findViewById<Button>(R.id.button_back)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
}