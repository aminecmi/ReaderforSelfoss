package apps.amine.bou.readerforselfoss

import android.view.View
import android.support.design.widget.TextInputLayout
import android.support.test.espresso.matcher.ViewMatchers

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.Matchers



fun isHintOrErrorEnabled(): Matcher<View> =
    object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description?) {}

        override fun matchesSafely(item: View?): Boolean {
            if (item !is TextInputLayout) {
                return false
            }

            return item.isHintEnabled || item.isErrorEnabled
        }
    }

fun withMenu(id: Int, titleId: Int): Matcher<View> =
    Matchers.anyOf(
        ViewMatchers.withId(id),
        ViewMatchers.withText(titleId)
    )
