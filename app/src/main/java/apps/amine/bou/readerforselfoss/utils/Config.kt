package apps.amine.bou.readerforselfoss.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import apps.amine.bou.readerforselfoss.LoginActivity

class Config(c: Context) {

    val settings: SharedPreferences = c.getSharedPreferences(settingsName, Context.MODE_PRIVATE)

    val baseUrl: String
        get() = settings.getString("url", "")

    val userLogin: String
        get() = settings.getString("login", "")

    val userPassword: String
        get() = settings.getString("password", "")

    val httpUserLogin: String
        get() = settings.getString("httpUserName", "")

    val httpUserPassword: String
        get() = settings.getString("httpPassword", "")

    companion object {
        val settingsName = "paramsselfoss"

        fun logoutAndRedirect(
                c: Context,
                callingActivity: Activity,
                editor: SharedPreferences.Editor,
                baseUrlFail: Boolean = false
        ): Boolean {
            editor.remove("url")
            editor.remove("login")
            editor.remove("password")
            editor.apply()
            val intent = Intent(c, LoginActivity::class.java)
            if (baseUrlFail) {
                intent.putExtra("baseUrlFail", baseUrlFail)
            }
            c.startActivity(intent)
            callingActivity.finish()
            return true
        }
    }
}
