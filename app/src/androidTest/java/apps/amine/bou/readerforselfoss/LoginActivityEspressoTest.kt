package apps.amine.bou.readerforselfoss

import android.content.Context
import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.closeSoftKeyboard
import android.support.test.espresso.action.ViewActions.pressBack
import android.support.test.espresso.action.ViewActions.typeText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.Intents.times
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.espresso.matcher.ViewMatchers.isRoot
import android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import apps.amine.bou.readerforselfoss.utils.Config
import com.mikepenz.aboutlibraries.ui.LibsActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityEspressoTest {

    @Rule @JvmField
    val rule = ActivityTestRule(LoginActivity::class.java, true, false)

    private lateinit var context: Context
    private lateinit var url: String
    private lateinit var username: String
    private lateinit var password: String

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

        onView(withId(R.id.loginProgress))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        onView(withId(R.id.urlView)).perform(click()).perform(typeText("WRONGURL"))

        onView(withId(R.id.signInButton)).perform(click())

        onView(withId(R.id.urlLayout)).check(matches(isHintOrErrorEnabled()))
    }

    // TODO: Add tests for multiple false urls with dialog

    @Test
    fun emptyAuthData() {

        rule.launchActivity(Intent())

        onView(withId(R.id.urlView)).perform(click()).perform(typeText(url), closeSoftKeyboard())

        onView(withId(R.id.withLogin)).perform(click())

        onView(withId(R.id.signInButton)).perform(click())

        onView(withId(R.id.loginLayout)).check(matches(isHintOrErrorEnabled()))
        onView(withId(R.id.passwordLayout)).check(matches(isHintOrErrorEnabled()))

        onView(withId(R.id.loginView)).perform(click()).perform(
                typeText(username),
                closeSoftKeyboard()
        )

        onView(withId(R.id.passwordLayout)).check(matches(isHintOrErrorEnabled()))

        onView(withId(R.id.signInButton)).perform(click())

        onView(withId(R.id.passwordLayout)).check(
                matches(
                        isHintOrErrorEnabled()
                )
        )
    }

    @Test
    fun wrongAuthData() {

        rule.launchActivity(Intent())

        onView(withId(R.id.urlView)).perform(click()).perform(typeText(url), closeSoftKeyboard())

        onView(withId(R.id.withLogin)).perform(click())

        onView(withId(R.id.loginView)).perform(click()).perform(
                typeText(username),
                closeSoftKeyboard()
        )

        onView(withId(R.id.passwordView)).perform(click()).perform(
                typeText("WRONGPASS"),
                closeSoftKeyboard()
        )

        onView(withId(R.id.signInButton)).perform(click())

        onView(withId(R.id.urlLayout)).check(matches(isHintOrErrorEnabled()))
        onView(withId(R.id.loginLayout)).check(matches(isHintOrErrorEnabled()))
        onView(withId(R.id.passwordLayout)).check(matches(isHintOrErrorEnabled()))
    }

    @Test
    fun workingAuth() {

        rule.launchActivity(Intent())

        onView(withId(R.id.urlView)).perform(click()).perform(typeText(url), closeSoftKeyboard())

        onView(withId(R.id.withLogin)).perform(click())

        onView(withId(R.id.loginView)).perform(click()).perform(
                typeText(username),
                closeSoftKeyboard()
        )

        onView(withId(R.id.passwordView)).perform(click()).perform(
                typeText(password),
                closeSoftKeyboard()
        )

        onView(withId(R.id.signInButton)).perform(click())

        intended(hasComponent(HomeActivity::class.java.name))
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }
}