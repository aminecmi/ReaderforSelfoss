package apps.amine.bou.readerforselfoss

import com.google.android.material.textfield.TextInputLayout
import androidx.test.espresso.matcher.ViewMatchers
import android.view.View
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher

fun isHintOrErrorEnabled(): Matcher<View> =
        object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description?) {
            }

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
