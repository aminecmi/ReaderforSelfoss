package apps.amine.bou.readerforselfoss.utils

import android.content.Context
import android.preference.PreferenceManager
import android.provider.Settings
import org.acra.ErrorReporter

fun ErrorReporter.maybeHandleSilentException(throwable: Throwable, ctx: Context) {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx)
    val isTestLab = Settings.System.getString(ctx.contentResolver, "firebase.test.lab") ==  "true"

    if (sharedPref.getBoolean("acra_should_log", false) && !isTestLab) {
        this.handleSilentException(throwable)
    }
}

fun ErrorReporter.doHandleSilentException(throwable: Throwable, ctx: Context) {
    val isTestLab = Settings.System.getString(ctx.contentResolver, "firebase.test.lab") ==  "true"
    if (!isTestLab) {
        this.handleSilentException(throwable)
    }
}