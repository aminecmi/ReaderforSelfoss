package apps.amine.bou.readerforselfoss.themes

import android.app.Activity
import android.content.Context
import android.preference.PreferenceManager
import androidx.annotation.ColorInt
import androidx.appcompat.view.ContextThemeWrapper
import android.util.TypedValue
import apps.amine.bou.readerforselfoss.R
import android.view.LayoutInflater
import android.view.ViewGroup

class AppColors(a: Activity) {

    @ColorInt val colorPrimary: Int
    @ColorInt val colorPrimaryDark: Int
    @ColorInt val colorAccent: Int
    @ColorInt val colorAccentDark: Int
    @ColorInt val cardBackgroundColor: Int
    @ColorInt val colorBackground: Int
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

        colorBackground = if (isDarkTheme) {
            a.setTheme(R.style.NoBarDark)
            R.color.darkBackground
        } else {
            a.setTheme(R.style.NoBar)
            android.R.color.background_light
        }

        val wrapper = Context::class.java
        val method = wrapper!!.getMethod("getThemeResId")
        method.isAccessible = true

        val typedCardBackground = TypedValue()
        a.theme.resolveAttribute(R.attr.cardBackgroundColor, typedCardBackground, true)

        cardBackgroundColor = typedCardBackground.data
    }
}
