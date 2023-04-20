package cz.dzubera.callwarden.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object PowerSaveUtils {

    fun checkIfIsRedmi(): Boolean {
        return Build.BRAND.lowercase() == "redmi"
    }

    //solution for other brands (oppo)
    //https://stackoverflow.com/questions/44383983/how-to-programmatically-enable-auto-start-and-floating-window-permissions
    fun navigateToAutoStartSetting(context: Context) {
        val intent = Intent();
        intent.setComponent(
            ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        )
        context.startActivity(intent);
    }

    fun checkIfPowerSaveOptimized(context: Context): Boolean {
        val manager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return manager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun navigateToPowerSave(context: Context) {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}")
            )
        )
    }
}