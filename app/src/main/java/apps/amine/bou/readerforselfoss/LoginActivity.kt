package apps.amine.bou.readerforselfoss

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.isBaseUrlValid
import com.crashlytics.android.Crashlytics
import com.ftinc.scoop.Scoop
import com.google.firebase.analytics.FirebaseAnalytics
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private var inValidCount: Int = 0
    private var isWithSelfSignedCert = false
    private var isWithLogin = false
    private var isWithHTTPLogin = false

    private lateinit var settings: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var userIdentifier: String
    private var logErrors: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scoop.getInstance().apply(this)
        setContentView(R.layout.activity_login)

        setSupportActionBar(toolbar)

        handleBaseUrlFail()


        settings = getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
        userIdentifier = settings.getString("unique_id", "")
        logErrors = settings.getBoolean("login_debug", false)

        editor = settings.edit()

        if (settings.getString("url", "").isNotEmpty()) {
            goToMain()
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        handleActions()
    }

    private fun handleActions() {

        withSelfhostedCert.setOnCheckedChangeListener { _, b ->
            isWithSelfSignedCert = !isWithSelfSignedCert
            val visi: Int = if (b) View.VISIBLE else View.GONE

            warningText.visibility = visi
        }

        passwordView.setOnEditorActionListener(
                TextView.OnEditorActionListener { _, id, _ ->
                    if (id == R.id.loginView || id == EditorInfo.IME_NULL) {
                        attemptLogin()
                        return@OnEditorActionListener true
                    }
                    false
                }
        )

        signInButton.setOnClickListener { attemptLogin() }

        withLogin.setOnCheckedChangeListener { _, b ->
            isWithLogin = !isWithLogin
            val visi: Int = if (b) View.VISIBLE else View.GONE

            loginLayout.visibility = visi
            passwordLayout.visibility = visi
        }

        withHttpLogin.setOnCheckedChangeListener { _, b ->
            isWithHTTPLogin = !isWithHTTPLogin
            val visi: Int = if (b) View.VISIBLE else View.GONE

            httpLoginInput.visibility = visi
            httpPasswordInput.visibility = visi
        }
    }

    private fun handleBaseUrlFail() {
        if (intent.getBooleanExtra("baseUrlFail", false)) {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle(getString(R.string.warning_wrong_url))
            alertDialog.setMessage(getString(R.string.base_url_error))
            alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL,
                    "OK",
                    { dialog, _ -> dialog.dismiss() }
            )
            alertDialog.show()
        }
    }

    private fun goToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun attemptLogin() {

        // Reset errors.
        urlView.error = null
        loginView.error = null
        httpLoginView.error = null
        passwordView.error = null
        httpPasswordView.error = null

        // Store values at the time of the login attempt.
        val url = urlView.text.toString()
        val login = loginView.text.toString()
        val httpLogin = httpLoginView.text.toString()
        val password = passwordView.text.toString()
        val httpPassword = httpPasswordView.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!url.isBaseUrlValid()) {
            urlView.error = getString(R.string.login_url_problem)
            focusView = urlView
            cancel = true
            inValidCount++
            if (inValidCount == 3) {
                val alertDialog = AlertDialog.Builder(this).create()
                alertDialog.setTitle(getString(R.string.warning_wrong_url))
                alertDialog.setMessage(getString(R.string.text_wrong_url))
                alertDialog.setButton(
                        AlertDialog.BUTTON_NEUTRAL,
                        "OK",
                        { dialog, _ -> dialog.dismiss() }
                )
                alertDialog.show()
                inValidCount = 0
            }
        }

        if (isWithLogin || isWithHTTPLogin) {
            if (TextUtils.isEmpty(password)) {
                passwordView.error = getString(R.string.error_invalid_password)
                focusView = passwordView
                cancel = true
            }

            if (TextUtils.isEmpty(login)) {
                loginView.error = getString(R.string.error_field_required)
                focusView = loginView
                cancel = true
            }
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            showProgress(true)

            editor.putString("url", url)
            editor.putString("login", login)
            editor.putString("httpUserName", httpLogin)
            editor.putString("password", password)
            editor.putString("httpPassword", httpPassword)
            editor.putBoolean("isSelfSignedCert", isWithSelfSignedCert)
            editor.apply()

            val api = SelfossApi(
                    this,
                    this@LoginActivity,
                    isWithSelfSignedCert,
                    isWithSelfSignedCert
            )
            api.login().enqueue(object : Callback<SuccessResponse> {
                private fun preferenceError(t: Throwable) {
                    editor.remove("url")
                    editor.remove("login")
                    editor.remove("httpUserName")
                    editor.remove("password")
                    editor.remove("httpPassword")
                    editor.apply()
                    urlView.error = getString(R.string.wrong_infos)
                    loginView.error = getString(R.string.wrong_infos)
                    passwordView.error = getString(R.string.wrong_infos)
                    httpLoginView.error = getString(R.string.wrong_infos)
                    httpPasswordView.error = getString(R.string.wrong_infos)
                    if (logErrors) {
                        Crashlytics.setUserIdentifier(userIdentifier)
                        Crashlytics.log(100, "LOGIN_DEBUG_ERRROR", t.message)
                        Crashlytics.logException(t)
                        Toast.makeText(
                                this@LoginActivity,
                                t.message,
                                Toast.LENGTH_LONG
                        ).show()
                    }
                    showProgress(false)
                }

                override fun onResponse(
                        call: Call<SuccessResponse>,
                        response: Response<SuccessResponse>
                ) {
                    if (response.body() != null && response.body()!!.isSuccess) {
                        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle())
                        goToMain()
                    } else {
                        preferenceError(Exception("No response body..."))
                    }
                }

                override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                    preferenceError(t)
                }
            })
        }
    }

    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)

        loginForm.visibility = if (show) View.GONE else View.VISIBLE
        loginForm
                .animate()
                .setDuration(shortAnimTime.toLong())
                .alpha(
                        if (show) 0F else 1F
                ).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        loginForm.visibility = if (show) View.GONE else View.VISIBLE
                    }
                }
        )

        loginProgress.visibility = if (show) View.VISIBLE else View.GONE
        loginProgress
                .animate()
                .setDuration(shortAnimTime.toLong())
                .alpha(
                        if (show) 1F else 0F
                ).setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        loginProgress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                }
        )
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.login_menu, menu)
        menu.findItem(R.id.login_debug).isChecked = logErrors
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.about -> {
                LibsBuilder()
                        .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                        .withAboutIconShown(true)
                        .withAboutVersionShown(true)
                        .start(this)
                return true
            }
            R.id.login_debug -> {
                val newState = !item.isChecked
                item.isChecked = newState
                logErrors = newState
                editor.putBoolean("login_debug", newState)
                editor.apply()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
