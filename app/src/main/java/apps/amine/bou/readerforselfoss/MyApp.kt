package apps.amine.bou.readerforselfoss

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.preference.PreferenceManager
import android.support.multidex.MultiDexApplication
import android.widget.ImageView
import com.anupcowkur.reservoir.Reservoir
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.ftinc.scoop.Scoop
import com.github.stkent.amplify.tracking.Amplify
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import io.fabric.sdk.android.Fabric
import java.io.IOException


class MyApp : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG)
            Fabric.with(this, Crashlytics())

        Amplify.initSharedInstance(this)
                .setFeedbackEmailAddress(getString(R.string.feedback_email))
                .setAlwaysShow(BuildConfig.DEBUG)
                .applyAllDefaultRules()

        try {
            Reservoir.init(this, 8192) //in bytes
        } catch (e: IOException) {
            //failure
        }

        DrawerImageLoader.init(object : AbstractDrawerImageLoader() {
            override fun set(imageView: ImageView?, uri: Uri?, placeholder: Drawable?, tag: String?) {
                Glide.with(imageView?.context).load(uri).placeholder(placeholder).into(imageView)
            }

            override fun cancel(imageView: ImageView?) {
                Glide.clear(imageView)
            }

            override fun placeholder(ctx: Context?, tag: String?): Drawable {
                return baseContext.resources.getDrawable(R.mipmap.ic_launcher)
            }
        })
        Scoop.waffleCone()
            .addFlavor("Default", R.style.NoBar, true)
            .addFlavor("NoBarTealOrange", R.style.NoBarTealOrange)
            .addFlavor("NoBarCyanPink", R.style.NoBarCyanPink)
            .addFlavor("NoBarGreyOrange", R.style.NoBarGreyOrange)
            .addFlavor("BlueAmber", R.style.NoBarBlueAmber)
            .addFlavor("NoBarIndigoPink", R.style.NoBarIndigoPink)
            .addFlavor("NoBarRedTeal", R.style.NoBarRedTeal)
            .addFlavor("Dark1", R.style.Dark1)
            .setSharedPreferences(PreferenceManager.getDefaultSharedPreferences(this))
            .initialize()

    }
}