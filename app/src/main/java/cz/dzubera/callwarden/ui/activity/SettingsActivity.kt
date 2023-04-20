package cz.dzubera.callwarden.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.utils.PowerSaveUtils

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        supportActionBar?.title = "Nastavení";

        val btnAutoStart = findViewById<CardView>(R.id.autostartView)
        val btnBatterySetting = findViewById<CardView>(R.id.batteryView)


        // Xiaomi redmi autostart
        if (PowerSaveUtils.checkIfIsRedmi()) {
            btnAutoStart.setOnClickListener {
               PowerSaveUtils.navigateToAutoStartSetting(this)
            }
        } else {
            btnAutoStart.visibility = View.GONE
        }

        // disable battery optimization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            btnBatterySetting.setOnClickListener {
                if (!PowerSaveUtils.checkIfPowerSaveOptimized(this)) {
                    PowerSaveUtils.navigateToPowerSave(this)
//                    startActivity(
//                        Intent(
//                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
//                            Uri.parse("package:$packageName")
//                        )
//                    )
                } else {
                    Toast.makeText(this, "Baterie je jíž nastavena", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            btnBatterySetting.visibility = View.GONE
        }

        // back button
        val backButton = findViewById<Button>(R.id.button_back)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }
}