package apps.amine.bou.readerforselfoss

import agency.tango.materialintroscreen.MaterialIntroActivity
import agency.tango.materialintroscreen.MessageButtonBehaviour
import agency.tango.materialintroscreen.SlideFragmentBuilder
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatDelegate
import android.view.View

class IntroActivity : MaterialIntroActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        addSlide(
                SlideFragmentBuilder()
                        .backgroundColor(R.color.colorPrimary)
                        .buttonsColor(R.color.colorAccent)
                        .image(R.drawable.web_hi_res_512)
                        .title(getString(R.string.intro_hello_title))
                        .description(getString(R.string.intro_hello_message))
                        .build()
        )

        addSlide(
                SlideFragmentBuilder()
                        .backgroundColor(R.color.colorAccent)
                        .buttonsColor(R.color.colorPrimary)
                        .image(R.drawable.ic_info_outline_white_48px)
                        .title(getString(R.string.intro_needs_selfoss_title))
                        .description(getString(R.string.intro_needs_selfoss_message))
                        .build(),
                MessageButtonBehaviour(
                        View.OnClickListener {
                            val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://selfoss.aditu.de")
                            )
                            startActivity(browserIntent)
                        }, getString(R.string.intro_needs_selfoss_link)
                )
        )

        addSlide(
                SlideFragmentBuilder()
                        .backgroundColor(R.color.colorPrimaryDark)
                        .buttonsColor(R.color.colorAccentDark)
                        .image(R.drawable.ic_thumb_up_white_48px)
                        .title(getString(R.string.intro_all_set_title))
                        .description(getString(R.string.intro_all_set_message))
                        .build()
        )
    }

    override fun onFinish() {
        super.onFinish()
        val getPrefs = PreferenceManager.getDefaultSharedPreferences(baseContext)
        val e = getPrefs.edit()
        e.putBoolean("firstStart", false)
        e.apply()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
