package apps.amine.bou.readerforselfoss.utils

import android.content.Context
import android.text.format.DateUtils
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossTagType
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun String.toTextDrawableString(c: Context): String {
    val textDrawable = StringBuilder()
    for (s in this.split(" ".toRegex()).filter { !it.isEmpty() }.toTypedArray()) {
        try {
            textDrawable.append(s[0])
        } catch (e: StringIndexOutOfBoundsException) {
        }
    }
    return textDrawable.toString()
}

fun Item.sourceAndDateText(): String {
    val formattedDate: String = try {
        var date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(this.datetime)
        // Api 3.0 changes the date format, check for ISO8601 format
        if (date == null) {
            date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").parse(this.datetime)
        }

        " " + DateUtils.getRelativeTimeSpanString(
            date.time,
            Date().time,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
    } catch (e: ParseException) {
        e.printStackTrace()
        ""
    }

    return this.getSourceTitle() + formattedDate
}

fun Item.toggleStar(): Item {
    this.starred = !this.starred
    return this
}

fun List<Item>.flattenTags(): List<Item> =
    this.flatMap {
        val item = it
        val tags: List<String> = it.tags.tags.split(",")
        tags.map { t ->
            item.copy(tags = SelfossTagType(t.trim()))
        }
    }