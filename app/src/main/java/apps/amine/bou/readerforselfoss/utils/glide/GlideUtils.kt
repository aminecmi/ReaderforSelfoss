package apps.amine.bou.readerforselfoss.utils.glide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import android.widget.ImageView
import apps.amine.bou.readerforselfoss.utils.Config
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget

fun Context.bitmapCenterCrop(config: Config, url: String, iv: ImageView) =
    Glide.with(this)
        .asBitmap()
        .loadMaybeBasicAuth(config, url)
        .apply(RequestOptions.centerCropTransform())
        .into(iv)

fun Context.circularBitmapDrawable(config: Config, url: String, iv: ImageView) =
    Glide.with(this)
        .asBitmap()
        .loadMaybeBasicAuth(config, url)
        .apply(RequestOptions.centerCropTransform())
        .into(object : BitmapImageViewTarget(iv) {
            override fun setResource(resource: Bitmap?) {
                val circularBitmapDrawable = RoundedBitmapDrawableFactory.create(
                    resources,
                    resource
                )
                circularBitmapDrawable.isCircular = true
                iv.setImageDrawable(circularBitmapDrawable)
            }
        })

fun RequestBuilder<Bitmap>.loadMaybeBasicAuth(config: Config, url: String): RequestBuilder<Bitmap> {
    val builder: LazyHeaders.Builder = LazyHeaders.Builder()
    if (config.httpUserLogin.isNotEmpty() || config.httpUserPassword.isNotEmpty()) {
        val basicAuth = "Basic " + Base64.encodeToString("${config.httpUserLogin}:${config.httpUserPassword}".toByteArray(), Base64.NO_WRAP)
        builder.addHeader("Authorization", basicAuth)
    }
    val glideUrl = GlideUrl(url, builder.build())
    return this.load(glideUrl)
}

fun RequestManager.loadMaybeBasicAuth(config: Config, url: String): RequestBuilder<Drawable> {
    val builder: LazyHeaders.Builder = LazyHeaders.Builder()
    if (config.httpUserLogin.isNotEmpty() || config.httpUserPassword.isNotEmpty()) {
        val basicAuth = "Basic " + Base64.encodeToString("${config.httpUserLogin}:${config.httpUserPassword}".toByteArray(), Base64.NO_WRAP)
        builder.addHeader("Authorization", basicAuth)
    }
    val glideUrl = GlideUrl(url, builder.build())
    return this.load(glideUrl)
}