package apps.amine.bou.readerforselfoss.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.support.v7.app.AlertDialog
import apps.amine.bou.readerforselfoss.R
import com.google.firebase.remoteconfig.FirebaseRemoteConfig

fun String?.isEmptyOrNullOrNullString(): Boolean =
        this == null || this == "null" || this.isEmpty()

fun Context.checkApkVersion(
        settings: SharedPreferences,
        editor: SharedPreferences.Editor,
        mFirebaseRemoteConfig: FirebaseRemoteConfig
) = {
    fun isThereAnUpdate() {
        val APK_LINK = "github_apk"

        val apkLink = mFirebaseRemoteConfig.getString(APK_LINK)
        val storedLink = settings.getString(APK_LINK, "")
        if (apkLink != storedLink && !apkLink.isEmpty()) {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle(getString(R.string.new_apk_available_title))
            alertDialog.setMessage(getString(R.string.new_apk_available_message))
            alertDialog.setButton(
                    AlertDialog.BUTTON_POSITIVE,
                    getString(R.string.new_apk_available_get)
            ) { _, _ ->
                editor.putString(APK_LINK, apkLink)
                editor.apply()
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkLink))
                startActivity(browserIntent)
            }
            alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL, getString(R.string.new_apk_available_no),
                    { dialog, _ ->
                        editor.putString(APK_LINK, apkLink)
                        editor.apply()
                        dialog.dismiss()
                    }
            )
            alertDialog.show()
        }
    }

    mFirebaseRemoteConfig.fetch(43200)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    mFirebaseRemoteConfig.activateFetched()
                }

                isThereAnUpdate()
            }
}

fun String.longHash(): Long {
    var h = 98764321261L
    val l = this.length
    val chars = this.toCharArray()

    for (i in 0 until l) {
        h = 31 * h + chars[i].toLong()
    }
    return h
}

fun String.toStringUriWithHttp(): String =
        if (!this.startsWith("https://") && !this.startsWith("http://")) {
            "http://" + this
        } else {
            this
        }

fun Context.shareLink(itemUrl: String) {
    val sendIntent = Intent()
    sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_TEXT, itemUrl.toStringUriWithHttp())
    sendIntent.type = "text/plain"
    startActivity(
            Intent.createChooser(
                    sendIntent,
                    getString(R.string.share)
            ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}