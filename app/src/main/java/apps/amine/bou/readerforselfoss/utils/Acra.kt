package apps.amine.bou.readerforselfoss.utils

import android.content.Context
import android.preference.PreferenceManager
import org.acra.ErrorReporter

fun ErrorReporter.maybeHandleSilentException(throwable: Throwable, ctx: Context) {
    val sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx)
    if (sharedPref.getBoolean("acra_should_log", false)) {
        this.handleSilentException(throwable)
    }
}