package cz.dzubera.callwarden.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cz.dzubera.callwarden.Config
import cz.dzubera.callwarden.R


class SignInActivity : AppCompatActivity() {

    val list = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.GET_ACCOUNTS,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_activity)

        checkPermissions()
        val manger = getSystemService(TELECOM_SERVICE) as TelecomManager

        val name: String = manger.defaultDialerPackage
        Log.d("PLS", "isDefault: $name")

        val user = getSharedPreferences("XXX", Context.MODE_PRIVATE).getString("userName", null)
        val number = getSharedPreferences("XXX", Context.MODE_PRIVATE).getString("userNumber", null)

        if (!user.isNullOrEmpty() && !number.isNullOrEmpty() && !Config.signedOut) {
            login()
            return
        }

        val userName = findViewById<TextView>(R.id.user_name)
        val userPhone = findViewById<TextView>(R.id.user_number)

        val loginButton = findViewById<Button>(R.id.user_log_in)
        loginButton.setOnClickListener {
            getSharedPreferences("XXX", Context.MODE_PRIVATE).edit()
                .putString("userName", userName.editableText.toString())
                .putString("userNumber", userPhone.editableText.toString()).apply()
            login()
        }


    }

    // Check permissions status
    private fun isPermissionsGranted(): Int {
        // PERMISSION_GRANTED : Constant Value: 0
        // PERMISSION_DENIED : Constant Value: -1
        var counter = 0;
        for (permission in list) {
            counter += ContextCompat.checkSelfPermission(this, permission)
        }
        return counter
    }


    // Find the first denied permission
    private fun deniedPermission(): String {
        for (permission in list) {
            if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_DENIED
            ) return permission
        }
        return ""
    }


    // Show alert dialog to request permissions
    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Need permission(s)")
        builder.setMessage("Some permissions are required to do the task.")
        builder.setPositiveButton("OK") { dialog, which -> requestPermissions() }
        builder.setNeutralButton("Cancel", null)
        val dialog = builder.create()
        dialog.show()
    }


    // Request the permissions at run time
    private fun requestPermissions() {
        val permission = deniedPermission()
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // Show an explanation asynchronously
            Toast.makeText(this, "Should show an explanation.", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, list.toTypedArray(), 29)
        }
    }


    // Process permissions result
    fun processPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ): Boolean {
        var result = 0
        if (grantResults.isNotEmpty()) {
            for (item in grantResults) {
                result += item
            }
        }
        if (result == PackageManager.PERMISSION_GRANTED) return true
        return false
    }

    fun checkPermissions(): Boolean {
        if (isPermissionsGranted() != PackageManager.PERMISSION_GRANTED) {
            showAlert()
            return false
        } else {
            return true
        }
    }

    private fun login() {
        Config.signedOut = false
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}