package apps.amine.bou.readerforselfoss

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.preference.PreferenceManager
import androidx.multidex.MultiDexApplication
import android.widget.ImageView
import apps.amine.bou.readerforselfoss.utils.Config
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ftinc.scoop.Scoop
import com.github.stkent.amplify.feedback.DefaultEmailFeedbackCollector
import com.github.stkent.amplify.feedback.GooglePlayStoreFeedbackCollector
import com.github.stkent.amplify.tracking.Amplify
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import org.acra.ACRA
import org.acra.ReportField
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraDialog
import org.acra.annotation.AcraHttpSender
import org.acra.sender.HttpSender
import java.io.IOException
import java.util.UUID.randomUUID


@AcraHttpSender(uri = "http://amine-bou.fr:5984/acra-selfoss/_design/acra-storage/_update/report",
                basicAuthLogin = "selfoss",
                basicAuthPassword = "selfoss",
                httpMethod = HttpSender.Method.PUT)
@AcraDialog(resText = R.string.crash_dialog_text,
            resCommentPrompt = R.string.crash_dialog_comment,
            resTheme = android.R.style.Theme_DeviceDefault_Dialog)
@AcraCore(reportContent = [ReportField.REPORT_ID, ReportField.INSTALLATION_ID,
    ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
    ReportField.BUILD, ReportField.ANDROID_VERSION, ReportField.BRAND, ReportField.PHONE_MODEL,
    ReportField.AVAILABLE_MEM_SIZE, ReportField.TOTAL_MEM_SIZE,
    ReportField.STACK_TRACE, ReportField.APPLICATION_LOG, ReportField.LOGCAT,
    ReportField.INITIAL_CONFIGURATION, ReportField.CRASH_CONFIGURATION, ReportField.IS_SILENT,
    ReportField.USER_APP_START_DATE, ReportField.USER_COMMENT, ReportField.USER_CRASH_DATE, ReportField.USER_EMAIL, ReportField.CUSTOM_DATA],
          buildConfigClass = BuildConfig::class)
class MyApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        initAmplify()

        val prefs = getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
        if (prefs.getString("unique_id", "").isEmpty()) {
            val editor = prefs.edit()
            editor.putString("unique_id", randomUUID().toString())
            editor.apply()
        }

        initDrawerImageLoader()

        initTheme()

        tryToHandleBug()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        val prefs = getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
        ACRA.init(this)
        ACRA.getErrorReporter().putCustomData("unique_id", prefs.getString("unique_id", ""))

    }

    private fun initAmplify() {
        Amplify.initSharedInstance(this)
            .setPositiveFeedbackCollectors(GooglePlayStoreFeedbackCollector())
            .setCriticalFeedbackCollectors(DefaultEmailFeedbackCollector(Config.feedbackEmail))
            .applyAllDefaultRules()
    }

    private fun initDrawerImageLoader() {
        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(
                imageView: ImageView?,
                uri: Uri?,
                placeholder: Drawable?,
                tag: String?
            ) {
                Glide.with(imageView?.context)
                    .load(uri)
                    .apply(RequestOptions.fitCenterTransform().placeholder(placeholder))
                    .into(imageView)
            }

            override fun cancel(imageView: ImageView?) {
                Glide.with(imageView?.context).clear(imageView)
            }

            override fun placeholder(ctx: Context?, tag: String?): Drawable {
                return baseContext.resources.getDrawable(R.mipmap.ic_launcher)
            }
        })
    }

    private fun initTheme() {
        Scoop.waffleCone()
            .addFlavor(getString(R.string.default_theme), R.style.NoBar, true)
            .addFlavor(getString(R.string.default_dark_theme), R.style.NoBarDark, false)
            .setSharedPreferences(PreferenceManager.getDefaultSharedPreferences(this))
            .initialize()
    }

    private fun tryToHandleBug() {
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            if (e is java.lang.NoClassDefFoundError && e.stackTrace.asList().any {
                    it.toString().contains("android.view.ViewDebug")
                }) {
                Unit
            } else {
                oldHandler.uncaughtException(thread, e)
            }
        }
    }
}