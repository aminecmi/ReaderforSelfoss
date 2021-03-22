package apps.amine.bou.readerforselfoss.api.selfoss

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.Html
import android.webkit.URLUtil
import org.jsoup.Jsoup

import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.isEmptyOrNullOrNullString
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.gson.annotations.SerializedName

private fun constructUrl(config: Config?, path: String, file: String?): String {
    return if (file.isEmptyOrNullOrNullString()) {
        ""
    } else {
        val baseUriBuilder = Uri.parse(config!!.baseUrl).buildUpon()
        baseUriBuilder.appendPath(path).appendPath(file)

        baseUriBuilder.toString()
    }
}

data class Tag(
    @SerializedName("tag") val tag: String,
    @SerializedName("color") val color: String,
    @SerializedName("unread") val unread: Int
)

class SuccessResponse(@SerializedName("success") val success: Boolean) {
    val isSuccess: Boolean
        get() = success
}

class Stats(
    @SerializedName("total") val total: Int,
    @SerializedName("unread") val unread: Int,
    @SerializedName("starred") val starred: Int
)

data class Spout(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String
)

data class ApiVersion(
        @SerializedName("version") val version: String?,
        @SerializedName("apiversion") val apiversion: String?
) {
    fun getApiMajorVersion() : Int {
        var versionNumber = 0
        if (apiversion != null) {
            versionNumber = apiversion.substringBefore(".").toInt()
        }
        return versionNumber
    }
}

data class Source(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("tags") val tags: SelfossTagType,
    @SerializedName("spout") val spout: String,
    @SerializedName("error") val error: String,
    @SerializedName("icon") val icon: String
) {
    var config: Config? = null

    fun getIcon(app: Context): String {
        if (config == null) {
            config = Config(app)
        }
        return constructUrl(config, "favicons", icon)
    }

    fun getTitleDecoded(): String {
        return Html.fromHtml(title).toString()
    }
}

data class Item(
    @SerializedName("id") val id: String,
    @SerializedName("datetime") val datetime: String,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("unread") val unread: Boolean,
    @SerializedName("starred") var starred: Boolean,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("icon") val icon: String?,
    @SerializedName("link") val link: String,
    @SerializedName("sourcetitle") val sourcetitle: String,
    @SerializedName("tags") val tags: SelfossTagType
) : Parcelable {

    var config: Config? = null

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Item> = object : Parcelable.Creator<Item> {
            override fun createFromParcel(source: Parcel): Item = Item(source)
            override fun newArray(size: Int): Array<Item?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(
        id = source.readString().orEmpty(),
        datetime = source.readString().orEmpty(),
        title = source.readString().orEmpty(),
        content = source.readString().orEmpty(),
        unread = 0.toByte() != source.readByte(),
        starred = 0.toByte() != source.readByte(),
        thumbnail = source.readString(),
        icon = source.readString(),
        link = source.readString().orEmpty(),
        sourcetitle = source.readString().orEmpty(),
        tags = if (source.readParcelable<SelfossTagType>(ClassLoader.getSystemClassLoader()) != null) source.readParcelable(ClassLoader.getSystemClassLoader())!! else SelfossTagType("")
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(id)
        dest.writeString(datetime)
        dest.writeString(title)
        dest.writeString(content)
        dest.writeByte((if (unread) 1 else 0))
        dest.writeByte((if (starred) 1 else 0))
        dest.writeString(thumbnail)
        dest.writeString(icon)
        dest.writeString(link)
        dest.writeString(sourcetitle)
        dest.writeParcelable(tags, flags)
    }

    fun getIcon(app: Context): String {
        if (config == null) {
            config = Config(app)
        }
        return constructUrl(config, "favicons", icon)
    }

    fun getThumbnail(app: Context): String {
        if (config == null) {
            config = Config(app)
        }
        return constructUrl(config, "thumbnails", thumbnail)
    }

    fun getImages() : ArrayList<String> {
        var allImages = ArrayList<String>()

        for ( image in Jsoup.parse(content).getElementsByTag("img")) {
            val url = image.attr("src")
            if (url.toLowerCase().contains(".jpg") ||
                    url.toLowerCase().contains(".jpeg") ||
                    url.toLowerCase().contains(".png") ||
                    url.toLowerCase().contains(".webp"))
            {
                allImages.add(url)
            }
        }
        return allImages
    }

    fun preloadImages(context: Context) : Boolean {
        val imageUrls = this.getImages()

        val glideOptions = RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.ALL).timeout(10000)


        try {
            for (url in imageUrls) {
                if ( URLUtil.isValidUrl(url)) {
                    val image = Glide.with(context).asBitmap()
                            .apply(glideOptions)
                            .load(url).submit()
                }
            }
        } catch (e : Error) {
            return false
        }

        return true
    }

    fun getTitleDecoded(): String {
        return Html.fromHtml(title).toString()
    }

    fun getSourceTitle(): String {
        return Html.fromHtml(sourcetitle).toString()
    }

    // TODO: maybe find a better way to handle these kind of urls
    fun getLinkDecoded(): String {
        var stringUrl: String
        stringUrl =
                if (link.startsWith("http://news.google.com/news/") || link.startsWith("https://news.google.com/news/")) {
                    if (link.contains("&amp;url=")) {
                        link.substringAfter("&amp;url=")
                    } else {
                        this.link.replace("&amp;", "&")
                    }
                } else {
                    this.link.replace("&amp;", "&")
                }

        // handle :443 => https
        if (stringUrl.contains(":443")) {
            stringUrl = stringUrl.replace(":443", "").replace("http://", "https://")
        }

        // handle url not starting with http
        if (stringUrl.startsWith("//")) {
            stringUrl = "http:$stringUrl"
        }

        return stringUrl
    }
}

data class SelfossTagType(val tags: String) : Parcelable {

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<SelfossTagType> =
            object : Parcelable.Creator<SelfossTagType> {
                override fun createFromParcel(source: Parcel): SelfossTagType =
                    SelfossTagType(source)

                override fun newArray(size: Int): Array<SelfossTagType?> = arrayOfNulls(size)
            }
    }

    constructor(source: Parcel) : this(
        tags = source.readString().orEmpty()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(tags)
    }
}