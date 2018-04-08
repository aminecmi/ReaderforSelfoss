package apps.amine.bou.readerforselfoss.themes

import android.app.Activity
import android.content.Context
import android.preference.PreferenceManager
import android.support.annotation.ColorInt
import android.util.TypedValue
import apps.amine.bou.readerforselfoss.R

class AppColors(a: Activity) {

    @ColorInt val colorPrimary: Int
    @ColorInt val colorPrimaryDark: Int
    @ColorInt val colorAccent: Int
    @ColorInt val colorAccentDark: Int
    @ColorInt val cardBackgroundColor: Int
    val isDarkTheme: Boolean

    init {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(a)

        colorPrimary =
                sharedPref.getInt(
                    "color_primary",
                    a.resources.getColor(R.color.colorPrimary)
                )
        colorPrimaryDark =
                sharedPref.getInt(
                    "color_primary_dark",
                    a.resources.getColor(R.color.colorPrimaryDark)
                )
        colorAccent =
                sharedPref.getInt(
                    "color_accent",
                    a.resources.getColor(R.color.colorAccent)
                )
        colorAccentDark =
                sharedPref.getInt(
                    "color_accent_dark",
                    a.resources.getColor(R.color.colorAccentDark)
                )
        isDarkTheme =
                sharedPref.getBoolean(
                    "dark_theme",
                    false
                )

        if (isDarkTheme) {
            a.setTheme(R.style.NoBarDark)
        } else {
            a.setTheme(R.style.NoBar)
        }

        val wrapper = Context::class.java
        val method = wrapper!!.getMethod("getThemeResId")
        method.isAccessible = true

        val typedCardBackground = TypedValue()
        a.theme.resolveAttribute(R.attr.cardBackgroundColor, typedCardBackground, true)

        cardBackgroundColor = typedCardBackground.data
    }
}
