package apps.amine.bou.readerforselfoss

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.preference.PreferenceManager
import android.support.multidex.MultiDexApplication
import android.widget.ImageView
import apps.amine.bou.readerforselfoss.utils.Config
import com.anupcowkur.reservoir.Reservoir
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.crashlytics.android.Crashlytics
import com.ftinc.scoop.Scoop
import com.github.stkent.amplify.feedback.DefaultEmailFeedbackCollector
import com.github.stkent.amplify.feedback.GooglePlayStoreFeedbackCollector
import com.github.stkent.amplify.tracking.Amplify
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import io.fabric.sdk.android.Fabric
import java.io.IOException
import java.util.UUID.randomUUID

class MyApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())

        initAmplify()

        initCache()

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

    private fun initAmplify() {
        Amplify.initSharedInstance(this)
                .setPositiveFeedbackCollectors(GooglePlayStoreFeedbackCollector())
                .setCriticalFeedbackCollectors(DefaultEmailFeedbackCollector(BuildConfig.FEEDBACK_EMAIL))
                .applyAllDefaultRules()
    }

    private fun initCache() {
        try {
            Reservoir.init(this, 8192) //in bytes
        } catch (e: IOException) {
            //failure
        }
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
                .addFlavor(getString(R.string.default_dark_theme), R.style.NoBarDark)
                .addFlavor(getString(R.string.teal_orange_theme), R.style.NoBarTealOrange)
                .addFlavor(getString(R.string.teal_orange_dark_theme), R.style.NoBarTealOrangeDark)
                .addFlavor(getString(R.string.cyan_pink_theme), R.style.NoBarCyanPink)
                .addFlavor(getString(R.string.cyan_pink_dark_theme), R.style.NoBarCyanPinkDark)
                .addFlavor(getString(R.string.grey_orange_theme), R.style.NoBarGreyOrange)
                .addFlavor(getString(R.string.grey_orange_dark_theme), R.style.NoBarGreyOrangeDark)
                .addFlavor(getString(R.string.blue_amber_theme), R.style.NoBarBlueAmber)
                .addFlavor(getString(R.string.blue_amber_dark_theme), R.style.NoBarBlueAmberDark)
                .addFlavor(getString(R.string.indigo_pink_theme), R.style.NoBarIndigoPink)
                .addFlavor(getString(R.string.indigo_pink_dark_theme), R.style.NoBarIndigoPinkDark)
                .addFlavor(getString(R.string.red_teal_theme), R.style.NoBarRedTeal)
                .addFlavor(getString(R.string.red_teal_dark_theme), R.style.NoBarRedTealDark)
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