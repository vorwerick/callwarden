package cz.dzubera.callwarden.utils

import android.content.Intent
import android.net.Uri

fun Uri.toIntent(): Intent {
    return Intent(Intent.ACTION_VIEW, this).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Nutné pro Xiaomi
    }
}