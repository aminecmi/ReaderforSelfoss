package apps.amine.bou.readerforselfoss

import android.content.Context
import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.Intents.times
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.KeyEvent

import com.mikepenz.aboutlibraries.ui.LibsActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import apps.amine.bou.readerforselfoss.settings.SettingsActivity
import apps.amine.bou.readerforselfoss.utils.Config
import org.junit.After


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

        onView(withId(R.id.search_src_text)).perform(typeText("android"), pressKey(KeyEvent.KEYCODE_SEARCH), closeSoftKeyboard())

        onView(withContentDescription(R.string.abc_toolbar_collapse_description)).perform(click())


        onView(withMenu(id = R.id.readAll, titleId = R.string.readAll)).perform(click())

        openActionBarOverflowOrOptionsMenu(context)

        onView(withMenu(id = R.id.refresh, titleId = R.string.menu_home_refresh))
            .perform(click())

        openActionBarOverflowOrOptionsMenu(context)

        onView(withText(R.string.action_disconnect)).perform(click())

        intended(hasComponent(LoginActivity::class.java.name), times(1))

        onView(isRoot()).perform(pressBack())


    }

    @Test
    fun drawerTesting() {

        rule.launchActivity(Intent())

        onView(withId(R.id.material_drawer_layout)).perform(DrawerActions.open())

        onView(withText(R.string.action_about)).perform(click())
        intended(hasComponent(LibsActivity::class.java.name))
        onView(isRoot()).perform(pressBack())
        intended(hasComponent(HomeActivity::class.java.name))

        onView(withId(R.id.material_drawer_layout)).perform(DrawerActions.open())

        onView(withId(R.id.material_drawer_layout)).perform(DrawerActions.open())
        onView(withText(R.string.drawer_action_clear)).perform(click())

        // bug
        //onView(withText(R.string.title_activity_settings)).perform(scrollTo(), click())
        //intended(hasComponent(SettingsActivity::class.java.name))

    }

    // TODO: test articles opening and actions for cards and lists

    @After
    fun releaseIntents() {
        Intents.release()
    }
}