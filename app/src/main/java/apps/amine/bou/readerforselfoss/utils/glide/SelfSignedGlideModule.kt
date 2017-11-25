package apps.amine.bou.readerforselfoss.utils.glide

import android.content.Context
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.getUnsafeHttpClient
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.GlideModule
import java.io.InputStream

class SelfSignedGlideModule : GlideModule {

    override fun applyOptions(context: Context?, builder: GlideBuilder?) {
    }

    override fun registerComponents(context: Context?, glide: Glide?, registry: Registry?) {

        if (context != null) {
            val pref = context?.getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
            if (pref.getBoolean("isSelfSignedCert", false)) {
                val client = getUnsafeHttpClient().build()

                registry?.append(
                        GlideUrl::class.java,
                        InputStream::class.java,
                        com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader.Factory(client)
                )
            }
        }
    }
}