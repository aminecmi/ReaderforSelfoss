package apps.amine.bou.readerforselfoss

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.TextInputLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView

import com.google.firebase.analytics.FirebaseAnalytics
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.checkAndDisplayStoreApk
import apps.amine.bou.readerforselfoss.utils.isUrlValid
import com.ftinc.scoop.Scoop


class LoginActivity : AppCompatActivity() {

    private var inValidCount: Int = 0
    private var isWithLogin = false
    private var isWithHTTPLogin = false

    private lateinit var settings: SharedPreferences
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mUrlView: EditText
    private lateinit var mLoginView: TextView
    private lateinit var mHTTPLoginView: TextView
    private lateinit var mProgressView: View
    private lateinit var mPasswordView: EditText
    private lateinit var mHTTPPasswordView: EditText
    private lateinit var mLoginFormView: View



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scoop.getInstance().apply(this)
        setContentView(R.layout.activity_login)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (intent.getBooleanExtra("baseUrlFail", false)) {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle(getString(R.string.warning_wrong_url))
            alertDialog.setMessage(getString(R.string.base_url_error))
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL,
                "OK",
                { dialog, _ -> dialog.dismiss() })
            alertDialog.show()
        }


        settings = getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
        if (settings.getString("url", "").isNotEmpty()) {
            goToMain()
        } else {
            this@LoginActivity.checkAndDisplayStoreApk()
        }

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mUrlView = findViewById(R.id.url)
        mLoginView = findViewById(R.id.login)
        mHTTPLoginView = findViewById(R.id.httpLogin)
        mPasswordView = findViewById(R.id.password)
        mHTTPPasswordView = findViewById(R.id.httpPassword)
        mLoginFormView = findViewById(R.id.login_form)
        mProgressView = findViewById(R.id.login_progress)

        val mSwitch: Switch = findViewById(R.id.withLogin)
        val mHTTPSwitch: Switch = findViewById(R.id.withHttpLogin)
        val mLoginLayout: TextInputLayout = findViewById(R.id.loginLayout)
        val mHTTPLoginLayout: TextInputLayout = findViewById(R.id.httpLoginInput)
        val mPasswordLayout: TextInputLayout = findViewById(R.id.passwordLayout)
        val mHTTPPasswordLayout: TextInputLayout = findViewById(R.id.httpPasswordInput)
        val mEmailSignInButton: Button = findViewById(R.id.email_sign_in_button)

        mPasswordView.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        mEmailSignInButton.setOnClickListener { attemptLogin() }

        mSwitch.setOnCheckedChangeListener { _, b ->
            isWithLogin = !isWithLogin
            val visi: Int = if (b) View.VISIBLE else View.GONE

            mLoginLayout.visibility = visi
            mPasswordLayout.visibility = visi
        }

        mHTTPSwitch.setOnCheckedChangeListener { _, b ->
            isWithHTTPLogin = !isWithHTTPLogin
            val visi: Int = if (b) View.VISIBLE else View.GONE

            mHTTPLoginLayout.visibility = visi
            mHTTPPasswordLayout.visibility = visi
        }
    }

    private fun goToMain() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun attemptLogin() {

        // Reset errors.
        mUrlView.error = null
        mLoginView.error = null
        mHTTPLoginView.error = null
        mPasswordView.error = null
        mHTTPPasswordView.error = null

        // Store values at the time of the login attempt.
        val url = mUrlView.text.toString()
        val login = mLoginView.text.toString()
        val httpLogin = mHTTPLoginView.text.toString()
        val password = mPasswordView.text.toString()
        val httpPassword = mHTTPPasswordView.text.toString()

        var cancel = false
        var focusView: View? = null

        if (!url.isUrlValid()) {
            mUrlView.error = getString(R.string.login_url_problem)
            focusView = mUrlView
            cancel = true
            inValidCount++
            if (inValidCount == 3) {
                val alertDialog = AlertDialog.Builder(this).create()
                alertDialog.setTitle(getString(R.string.warning_wrong_url))
                alertDialog.setMessage(getString(R.string.text_wrong_url))
                alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL,
                    "OK",
                    { dialog, _ -> dialog.dismiss() })
                alertDialog.show()
                inValidCount = 0
            }
        }

        if (isWithLogin || isWithHTTPLogin) {
            if (TextUtils.isEmpty(password)) {
                mPasswordView.error = getString(R.string.error_invalid_password)
                focusView = mPasswordView
                cancel = true
            }

            if (TextUtils.isEmpty(login)) {
                mLoginView.error = getString(R.string.error_field_required)
                focusView = mLoginView
                cancel = true
            }
        }

        if (cancel) {
            focusView?.requestFocus()
        } else {
            showProgress(true)

            val editor = settings.edit()
            editor.putString("url", url)
            editor.putString("login", login)
            editor.putString("httpUserName", httpLogin)
            editor.putString("password", password)
            editor.putString("httpPassword", httpPassword)
            editor.apply()

            val api = SelfossApi(this, this@LoginActivity)
            api.login().enqueue(object : Callback<SuccessResponse> {
                private fun preferenceError() {
                    editor.remove("url")
                    editor.remove("login")
                    editor.remove("httpUserName")
                    editor.remove("password")
                    editor.remove("httpPassword")
                    editor.apply()
                    mUrlView.error = getString(R.string.wrong_infos)
                    mLoginView.error = getString(R.string.wrong_infos)
                    mPasswordView.error = getString(R.string.wrong_infos)
                    mHTTPLoginView.error = getString(R.string.wrong_infos)
                    mHTTPPasswordView.error = getString(R.string.wrong_infos)
                    showProgress(false)
                }

                override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {
                    if (response.body() != null && response.body()!!.isSuccess) {
                        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle())
                        goToMain()
                    } else {
                        preferenceError()
                    }
                }

                override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                    preferenceError()
                }
            })
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)

        mLoginFormView.visibility = if (show) View.GONE else View.VISIBLE
        mLoginFormView
            .animate()
            .setDuration(shortAnimTime.toLong())
            .alpha(
                if (show) 0F else 1F
            ).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mLoginFormView.visibility = if (show) View.GONE else View.VISIBLE
                }
            })

        mProgressView.visibility = if (show) View.VISIBLE else View.GONE
        mProgressView
            .animate()
            .setDuration(shortAnimTime.toLong())
            .alpha(
                if (show) 1F else 0F
            ).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    mProgressView.visibility = if (show) View.VISIBLE else View.GONE
                }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.login_menu, menu)
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
            else -> return super.onOptionsItemSelected(item)
        }
    }
}
