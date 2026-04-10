package cz.dzubera.callwarden.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.utils.PowerSaveUtils
import cz.dzubera.callwarden.utils.PreferencesUtils
import java.lang.Math.abs

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        supportActionBar?.title = getString(R.string.settings_title)

        val btnAutoStart = findViewById<CardView>(R.id.autostartView)
        val btnBatterySetting = findViewById<CardView>(R.id.batteryView)
        val btnSyncCount = findViewById<CardView>(R.id.syncCount)
        val syncCount = findViewById<TextView>(R.id.syncCountText)

        syncCount.text = PreferencesUtils.loadSyncCount(this).toString()

        btnSyncCount.setOnClickListener {
            showSyncCountDialog()
        }

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
                    Toast.makeText(
                        this,
                        getString(R.string.battery_already_set),
                        Toast.LENGTH_SHORT
                    ).show()
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

    //show native alert dialog with edit text to change sync count
    private fun showSyncCountDialog() {
        val currentVal = PreferencesUtils.loadSyncCount(this)
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.sync_count_title))

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.setText(currentVal.toString())
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.save)) { dialog, which ->
            var x = kotlin.math.abs(input.text.toString().toIntOrNull() ?: 100)
            if(x == 0){
                x = 1
            }
            if(x >= 500){
                x = 500
            }
            PreferencesUtils.saveSyncCount(this, x)
            Toast.makeText(
                this,
                getString(R.string.sync_count_saved, x),
                Toast.LENGTH_SHORT
            ).show()
            findViewById<TextView>(R.id.syncCountText).text = x.toString()
            dialog.cancel()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialog, _ -> dialog.cancel() }

        builder.show()
    }

}