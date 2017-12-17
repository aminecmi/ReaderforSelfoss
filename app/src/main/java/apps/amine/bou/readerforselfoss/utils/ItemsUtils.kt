package apps.amine.bou.readerforselfoss.utils

import android.text.format.DateUtils
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

fun String.toTextDrawableString(): String {
    val textDrawable = StringBuilder()
    for (s in this.split(" ".toRegex()).filter { !it.isEmpty() }.toTypedArray()) {
        textDrawable.append(s[0])
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