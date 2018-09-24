package apps.amine.bou.readerforselfoss

import android.content.Context
import android.content.Intent
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressKey
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import android.view.KeyEvent
import apps.amine.bou.readerforselfoss.utils.Config
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeActivityEspressoTest {
    lateinit var context: Context

    @Rule @JvmField
    val rule = ActivityTestRule(HomeActivity::class.java, true, false)

    @Before
    fun clearData() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val editor =
                context
                        .getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
                        .edit()
        editor.clear()

        editor.putString("url", BuildConfig.LOGIN_URL)
        editor.putString("login", BuildConfig.LOGIN_USERNAME)
        editor.putString("password", BuildConfig.LOGIN_PASSWORD)

        editor.commit()

        Intents.init()
    }

    @Test
    fun menuItems() {

        rule.launchActivity(Intent())

        onView(
                withMenu(
                        id = R.id.action_search,
                        titleId = R.string.menu_home_search
                )
        ).perform(click())

        onView(withId(R.id.search_bar)).check(matches(isDisplayed()))

        onView(withId(R.id.search_src_text)).perform(
                typeText("android"),
                pressKey(KeyEvent.KEYCODE_SEARCH),
                closeSoftKeyboard()
        )

        onView(withContentDescription(R.string.abc_toolbar_collapse_description)).perform(click())

        openActionBarOverflowOrOptionsMenu(context)

        onView(withMenu(id = R.id.refresh, titleId = R.string.menu_home_refresh))
                .perform(click())

        openActionBarOverflowOrOptionsMenu(context)

        onView(withText(R.string.action_disconnect)).perform(click())

        intended(hasComponent(LoginActivity::class.java.name), times(1))
    }

    // TODO: test articles opening and actions for cards and lists

    @After
    fun releaseIntents() {
        Intents.release()
    }
}