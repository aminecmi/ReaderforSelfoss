package apps.amine.bou.readerforselfoss

import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.intent.Intents
import android.support.test.espresso.intent.Intents.intended
import android.support.test.espresso.intent.Intents.times
import android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.After

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainActivityEspressoTest {

    lateinit var intent: Intent
    lateinit var preferencesEditor: SharedPreferences.Editor

    @Rule @JvmField
    val rule = ActivityTestRule(MainActivity::class.java, true, false)

    @Before
    fun setUp() {
        intent = Intent()
        val context = getInstrumentation().targetContext

        // create a SharedPreferences editor
        preferencesEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        Intents.init()
    }

    @Test
    fun checkFirstOpenLaunchesIntro() {
        preferencesEditor.putBoolean("firstStart", true)
        preferencesEditor.commit()

        rule.launchActivity(intent)

        intended(hasComponent(MainActivity::class.java.name))
        intended(hasComponent(IntroActivity::class.java.name))
        intended(hasComponent(LoginActivity::class.java.name), times(0))

    }

    @Test
    fun checkNotFirstOpenLaunchesLogin() {
        preferencesEditor.putBoolean("firstStart", false)
        preferencesEditor.commit()

        rule.launchActivity(intent)

        intended(hasComponent(MainActivity::class.java.name))
        intended(hasComponent(LoginActivity::class.java.name))
        intended(hasComponent(IntroActivity::class.java.name), times(0))

    }

    @After
    fun releaseIntents() {
        Intents.release()
    }


}