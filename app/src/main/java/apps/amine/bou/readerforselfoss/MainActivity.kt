package apps.amine.bou.readerforselfoss

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (PreferenceManager.getDefaultSharedPreferences(baseContext).getBoolean(
                "firstStart",
                true
        )) {
            val i = Intent(this@MainActivity, IntroActivity::class.java)
            startActivity(i)
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        finish()
    }
}
