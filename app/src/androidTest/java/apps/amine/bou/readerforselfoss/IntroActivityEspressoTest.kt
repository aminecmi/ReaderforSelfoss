package apps.amine.bou.readerforselfoss

import java.util.*

import android.content.Context
import android.content.Intent
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.Intents.times
import android.support.test.espresso.intent.matcher.IntentMatchers.*
import android.support.test.espresso.intent.matcher.UriMatchers.hasHost
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4

import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import apps.amine.bou.readerforselfoss.utils.Config
import org.junit.After


@RunWith(AndroidJUnit4::class)
class IntroActivityEspressoTest {

    @Rule @JvmField
    val rule = ActivityTestRule(IntroActivity::class.java, true, false)

    @Before
    fun clearData() {
        val editor =
            getInstrumentation().targetContext
                .getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
                .edit()
        editor.clear()
        editor.commit()

        Intents.init()
    }

    @Test
    fun nextEachTimes() {

        rule.launchActivity(Intent())

        onView(withText(R.string.intro_hello_title)).check(matches(isDisplayed()))
        onView(withId(R.id.button_next)).perform(click())
        onView(withText(R.string.intro_needs_selfoss_message)).check(matches(isDisplayed()))
        onView(withId(R.id.button_next)).perform(click())
        onView(withText(R.string.intro_all_set_message)).check(matches(isDisplayed()))
        onView(withId(R.id.button_next)).perform(click())

        intended(hasComponent(IntroActivity::class.java.name), times(1))
        intended(hasComponent(LoginActivity::class.java.name), times(1))

    }

    @Test
    fun nextBackRandomTimes() {
        val max = 5
        val min = 1

        val random = (Random().nextInt(max + 1 - min)) + min

        rule.launchActivity(Intent())

        onView(withText(R.string.intro_hello_title)).check(matches(isDisplayed()))
        onView(withId(R.id.button_next)).perform(click())

        repeat(random) {_ ->
            onView(withText(R.string.intro_needs_selfoss_message)).check(matches(isDisplayed()))
            onView(withId(R.id.button_next)).perform(click())
            onView(withText(R.string.intro_all_set_message)).check(matches(isDisplayed()))
            onView(withId(R.id.button_back)).perform(click())
        }

        onView(withId(R.id.button_next)).perform(click())
        onView(withText(R.string.intro_all_set_message)).check(matches(isDisplayed()))
        onView(withId(R.id.button_next)).perform(click())

        intended(hasComponent(IntroActivity::class.java.name), times(1))
        intended(hasComponent(LoginActivity::class.java.name), times(1))

    }

    @Test
    fun clickSelfossUrl() {
        rule.launchActivity(Intent())

        onView(withText(R.string.intro_hello_title)).check(matches(isDisplayed()))

        onView(withId(R.id.button_next)).perform(click())

        onView(withId(R.id.button_message)).perform(click())

        intended(
            allOf(
                hasData(
                    hasHost(
                        equalTo("selfoss.aditu.de")
                    )
                ),
                hasAction(Intent.ACTION_VIEW)
            )
        )

    }

    @After
    fun releaseIntents() {
        Intents.release()
    }
}