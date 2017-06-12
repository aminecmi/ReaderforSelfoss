package apps.amine.bou.readerforselfoss

import android.content.Context
import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.Intents.times
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4

import com.mikepenz.aboutlibraries.ui.LibsActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import apps.amine.bou.readerforselfoss.utils.Config
import org.junit.After


@RunWith(AndroidJUnit4::class)
class LoginActivityEspressoTest {

    @Rule @JvmField
    val rule = ActivityTestRule(LoginActivity::class.java, true, false)

    lateinit var context: Context
    lateinit var url: String
    lateinit var username: String
    lateinit var password: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val editor =
            context
                .getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
                .edit()
        editor.clear()
        editor.commit()


        url = BuildConfig.LOGIN_URL
        username = BuildConfig.LOGIN_USERNAME
        password = BuildConfig.LOGIN_PASSWORD

        Intents.init()
    }

    @Test
    fun menuItems() {

        rule.launchActivity(Intent())

        openActionBarOverflowOrOptionsMenu(context)

        onView(withText(R.string.action_about)).perform(click())

        intended(hasComponent(LibsActivity::class.java.name), times(1))

        onView(isRoot()).perform(pressBack())

        intended(hasComponent(LoginActivity::class.java.name))

    }


    @Test
    fun wrongLoginUrl() {
        rule.launchActivity(Intent())

        onView(withId(R.id.login_progress))
            .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        onView(withId(R.id.url)).perform(click()).perform(typeText("WRONGURL"))

        onView(withId(R.id.email_sign_in_button)).perform(click())

        onView(withId(R.id.urlLayout)).check(matches(isHintOrErrorEnabled()))
    }

    // TODO: Add tests for multiple false urls with dialog

    @Test
    fun emptyAuthData() {

        rule.launchActivity(Intent())

        onView(withId(R.id.url)).perform(click()).perform(typeText(url), closeSoftKeyboard())

        onView(withId(R.id.withLogin)).perform(click())

        onView(withId(R.id.email_sign_in_button)).perform(click())

        onView(withId(R.id.loginLayout)).check(matches(isHintOrErrorEnabled()))
        onView(withId(R.id.passwordLayout)).check(matches(isHintOrErrorEnabled()))

        onView(withId(R.id.login)).perform(click()).perform(typeText(username), closeSoftKeyboard())

        onView(withId(R.id.passwordLayout)).check(matches(isHintOrErrorEnabled()))

        onView(withId(R.id.email_sign_in_button)).perform(click())

        onView(withId(R.id.passwordLayout)).check(
            matches(
                isHintOrErrorEnabled())
        )

    }

    @Test
    fun wrongAuthData() {

        rule.launchActivity(Intent())

        onView(withId(R.id.url)).perform(click()).perform(typeText(url), closeSoftKeyboard())

        onView(withId(R.id.withLogin)).perform(click())

        onView(withId(R.id.login)).perform(click()).perform(typeText(username), closeSoftKeyboard())

        onView(withId(R.id.password)).perform(click()).perform(typeText("WRONGPASS"), closeSoftKeyboard())

        onView(withId(R.id.email_sign_in_button)).perform(click())

        onView(withId(R.id.urlLayout)).check(matches(isHintOrErrorEnabled()))
        onView(withId(R.id.loginLayout)).check(matches(isHintOrErrorEnabled()))
        onView(withId(R.id.passwordLayout)).check(matches(isHintOrErrorEnabled()))

    }

    @Test
    fun workingAuth() {

        rule.launchActivity(Intent())

        onView(withId(R.id.url)).perform(click()).perform(typeText(url), closeSoftKeyboard())

        onView(withId(R.id.withLogin)).perform(click())

        onView(withId(R.id.login)).perform(click()).perform(typeText(username), closeSoftKeyboard())

        onView(withId(R.id.password)).perform(click()).perform(typeText(password), closeSoftKeyboard())

        onView(withId(R.id.email_sign_in_button)).perform(click())

        intended(hasComponent(HomeActivity::class.java.name))

    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

}