package apps.amine.bou.readerforselfoss.utils

import android.text.format.DateUtils
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import com.crashlytics.android.Crashlytics
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun String.toTextDrawableString(): String {
    val textDrawable = StringBuilder()
    for (s in this.split(" ".toRegex()).filter { !it.isEmpty() }.toTypedArray()) {
        try {
            textDrawable.append(s[0])
        } catch (e: StringIndexOutOfBoundsException) {
            Crashlytics.log(
                100,
                "TEXT_DRAWABLE_INDEX_OUT_OF_BOUND",
                this + " produces ${e.message}"
            )
            Crashlytics.logException(e)
        }
    }
    return textDrawable.toString()
}

fun Item.sourceAndDateText(): String {
    val formattedDate: String = try {
        " " + DateUtils.getRelativeTimeSpanString(
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(this.datetime).time,
            Date().time,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        )
    } catch (e: ParseException) {
        e.printStackTrace()
        ""
    }

    return this.sourcetitle + formattedDate
}

fun Item.toggleStar(): Item {
    this.starred = !this.starred
    return this
}