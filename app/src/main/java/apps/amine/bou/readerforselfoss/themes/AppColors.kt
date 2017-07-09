package apps.amine.bou.readerforselfoss.themes

import android.app.Activity
import android.content.Context
import android.support.annotation.ColorInt
import android.util.TypedValue
import apps.amine.bou.readerforselfoss.R
import java.lang.reflect.AccessibleObject.setAccessible



class AppColors(a: Activity) {
    @ColorInt val accent: Int
    @ColorInt val dark: Int
    @ColorInt val primary: Int
    @ColorInt val cardBackground: Int
    @ColorInt val windowBackground: Int
    val isDarkTheme: Boolean

    init {
        val wrapper = Context::class.java
        val method = wrapper!!.getMethod("getThemeResId")
        method.isAccessible = true

        isDarkTheme = when(method.invoke(a.baseContext)) {
            R.style.NoBarTealOrangeDark,
            R.style.NoBarDark,
            R.style.NoBarBlueAmberDark,
            R.style.NoBarGreyOrangeDark,
            R.style.NoBarIndigoPinkDark,
            R.style.NoBarRedTealDark,
            R.style.NoBarCyanPinkDark -> true
            else -> false
        }

        val typedAccent = TypedValue()
        val typedAccentDark = TypedValue()
        val typedPrimary = TypedValue()
        val typedCardBackground = TypedValue()
        val typedWindowBackground = TypedValue()

        a.theme.resolveAttribute(R.attr.colorAccent, typedAccent, true)
        a.theme.resolveAttribute(R.attr.colorAccent, typedAccent, true)
        a.theme.resolveAttribute(R.attr.colorPrimary, typedPrimary, true)
        a.theme.resolveAttribute(R.attr.cardBackgroundColor, typedCardBackground, true)
        a.theme.resolveAttribute(android.R.attr.colorBackground, typedWindowBackground, true)
        accent = typedAccent.data
        dark = typedAccentDark.data
        primary = typedPrimary.data
        cardBackground = typedCardBackground.data
        windowBackground = typedWindowBackground.data
    }
}
