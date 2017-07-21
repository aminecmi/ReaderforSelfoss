package apps.amine.bou.readerforselfoss.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.support.v7.app.AlertDialog
import android.text.TextUtils
import android.util.Patterns

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import okhttp3.HttpUrl

import apps.amine.bou.readerforselfoss.BuildConfig
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.Item


fun Context.checkAndDisplayStoreApk() = {
    fun isStoreVersion(): Boolean =
        try {
            val installer = this.packageManager
                .getInstallerPackageName(this.packageName)
            !TextUtils.isEmpty(installer)
        } catch (e: Throwable) {
            false
        }

    if (!isStoreVersion() && !BuildConfig.GITHUB_VERSION) {
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.setTitle(getString(R.string.warning_version))
        alertDialog.setMessage(getString(R.string.text_version))
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
            { dialog, _ -> dialog.dismiss() })
        alertDialog.show()
    } else Unit
}

fun String.isUrlValid(): Boolean =
    HttpUrl.parse(this) != null && Patterns.WEB_URL.matcher(this).matches()

fun String.isBaseUrlValid(): Boolean {
    val baseUrl = HttpUrl.parse(this)
    var existsAndEndsWithSlash = false
    if (baseUrl != null) {
        val pathSegments = baseUrl.pathSegments()
        existsAndEndsWithSlash = "" == pathSegments[pathSegments.size - 1]
    }

    return Patterns.WEB_URL.matcher(this).matches() && existsAndEndsWithSlash
}

fun String?.isEmptyOrNullOrNullString(): Boolean =
    this == null || this == "null" || this.isEmpty()

fun Context.checkApkVersion(settings: SharedPreferences,
                    editor: SharedPreferences.Editor,
                    mFirebaseRemoteConfig: FirebaseRemoteConfig) = {
    fun isThereAnUpdate() {
        val APK_LINK = "github_apk"

        val apkLink = mFirebaseRemoteConfig.getString(APK_LINK)
        val storedLink = settings.getString(APK_LINK, "")
        if (apkLink != storedLink && !apkLink.isEmpty()) {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle(getString(R.string.new_apk_available_title))
            alertDialog.setMessage(getString(R.string.new_apk_available_message))
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.new_apk_available_get)) { _, _ ->
                editor.putString(APK_LINK, apkLink)
                editor.apply()
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkLink))
                startActivity(browserIntent)
            }
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getString(R.string.new_apk_available_no),
                { dialog, _ ->
                    editor.putString(APK_LINK, apkLink)
                    editor.apply()
                    dialog.dismiss()
                })
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

    for (i in 0..l - 1) {
        h = 31 * h + chars[i].toLong()
    }
    return h
}

fun String.toStringUriWithHttp() =
    if (!this.startsWith("https://") && !this.startsWith("http://"))
        "http://" + this
    else
        this

fun Context.shareLink(itemUrl: String) {
    val sendIntent = Intent()
    sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_TEXT, itemUrl.toStringUriWithHttp())
    sendIntent.type = "text/plain"
    startActivity(Intent.createChooser(sendIntent, getString(R.string.share)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun Context.openInBrowser(i: Item) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    intent.data = Uri.parse(i.getLinkDecoded().toStringUriWithHttp())
    startActivity(intent)
}