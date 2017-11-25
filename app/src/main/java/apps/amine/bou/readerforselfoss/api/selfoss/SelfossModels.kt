package apps.amine.bou.readerforselfoss.api.selfoss

import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.isEmptyOrNullOrNullString
import com.google.gson.annotations.SerializedName

private fun constructUrl(config: Config?, path: String, file: String): String {
    val baseUriBuilder = Uri.parse(config!!.baseUrl).buildUpon()
    baseUriBuilder.appendPath(path).appendPath(file)

    return if (file.isEmptyOrNullOrNullString()) {
        ""
    } else {
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

data class Sources(
        @SerializedName("id") val id: String,
        @SerializedName("title") val title: String,
        @SerializedName("tags") val tags: String,
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
}

data class Item(
        @SerializedName("id") val id: String,
        @SerializedName("datetime") val datetime: String,
        @SerializedName("title") val title: String,
        @SerializedName("content") val content: String,
        @SerializedName("unread") val unread: Boolean,
        @SerializedName("starred") val starred: Boolean,
        @SerializedName("thumbnail") val thumbnail: String,
        @SerializedName("icon") val icon: String,
        @SerializedName("link") val link: String,
        @SerializedName("sourcetitle") val sourcetitle: String
) : Parcelable {

    var config: Config? = null

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Item> = object : Parcelable.Creator<Item> {
            override fun createFromParcel(source: Parcel): Item = Item(source)
            override fun newArray(size: Int): Array<Item?> = arrayOfNulls(size)
        }
    }

    constructor(source: Parcel) : this(
            id = source.readString(),
            datetime = source.readString(),
            title = source.readString(),
            content = source.readString(),
            unread = 0.toByte() != source.readByte(),
            starred = 0.toByte() != source.readByte(),
            thumbnail = source.readString(),
            icon = source.readString(),
            link = source.readString(),
            sourcetitle = source.readString()
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

    // TODO: maybe find a better way to handle these kind of urls
    fun getLinkDecoded(): String {
        var stringUrl: String
        stringUrl = if (link.startsWith("http://news.google.com/news/") || link.startsWith("https://news.google.com/news/")) {
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
            stringUrl = "http:" + stringUrl
        }

        return stringUrl
    }
}