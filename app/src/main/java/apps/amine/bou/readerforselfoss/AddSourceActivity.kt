package apps.amine.bou.readerforselfoss

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.Spout
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.themes.Toppings
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.isBaseUrlValid
import com.ftinc.scoop.Scoop
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.graphics.PorterDuff
import apps.amine.bou.readerforselfoss.databinding.ActivityAddSourceBinding



class AddSourceActivity : AppCompatActivity() {

    private var mSpoutsValue: String? = null
    private lateinit var api: SelfossApi

    private lateinit var appColors: AppColors
    private lateinit var binding: ActivityAddSourceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        appColors = AppColors(this@AddSourceActivity)

        super.onCreate(savedInstanceState)
        binding = ActivityAddSourceBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        val scoop = Scoop.getInstance()
        scoop.bind(this, Toppings.PRIMARY.value, binding.toolbar)
        if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scoop.bindStatusBar(this, Toppings.PRIMARY_DARK.value)
        }

        val drawable = binding.nameInput.background
        drawable.setColorFilter(appColors.colorAccent, PorterDuff.Mode.SRC_ATOP)


        // TODO: clean
        if(Build.VERSION.SDK_INT > 16) {
            binding.nameInput.background = drawable
        } else{
            binding.nameInput.setBackgroundDrawable(drawable)
        }

        val drawable1 = binding.sourceUri.background
        drawable1.setColorFilter(appColors.colorAccent, PorterDuff.Mode.SRC_ATOP)

        if(Build.VERSION.SDK_INT > 16) {
            binding.sourceUri.background = drawable1
        } else{
            binding.sourceUri.setBackgroundDrawable(drawable1)
        }

        val drawable2 = binding.tags.background
        drawable2.setColorFilter(appColors.colorAccent, PorterDuff.Mode.SRC_ATOP)

        if(Build.VERSION.SDK_INT > 16) {
            binding.tags.background = drawable2
        } else{
            binding.tags.setBackgroundDrawable(drawable2)
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val settings =
                getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
            api = SelfossApi(
                this,
                this@AddSourceActivity,
                settings.getBoolean("isSelfSignedCert", false),
                prefs.getString("api_timeout", "-1")!!.toLong()
            )
        } catch (e: IllegalArgumentException) {
            mustLoginToAddSource()
        }

        maybeGetDetailsFromIntentSharing(intent, binding.sourceUri, binding.nameInput)

        binding.saveBtn.setTextColor(appColors.colorAccent)

        binding.saveBtn.setOnClickListener {
            handleSaveSource(binding.tags, binding.nameInput.text.toString(), binding.sourceUri.text.toString(), api)
        }
    }

    override fun onResume() {
        super.onResume()
        val config = Config(this)

        if (config.baseUrl.isEmpty() || !config.baseUrl.isBaseUrlValid(this@AddSourceActivity)) {
            mustLoginToAddSource()
        } else {
            handleSpoutsSpinner(binding.spoutsSpinner, api, binding.progress, binding.formContainer)
        }
    }

    private fun handleSpoutsSpinner(
        spoutsSpinner: Spinner,
        api: SelfossApi?,
        mProgress: ProgressBar,
        formContainer: ConstraintLayout
    ) {
        val spoutsKV = HashMap<String, String>()
        spoutsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                if (view != null) {
                    val spoutName = (view as TextView).text.toString()
                    mSpoutsValue = spoutsKV[spoutName]
                }
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {
                mSpoutsValue = null
            }
        }

        var items: Map<String, Spout>
        api!!.spouts().enqueue(object : Callback<Map<String, Spout>> {
            override fun onResponse(
                call: Call<Map<String, Spout>>,
                response: Response<Map<String, Spout>>
            ) {
                if (response.body() != null) {
                    items = response.body()!!

                    val itemsStrings = items.map { it.value.name }
                    for ((key, value) in items) {
                        spoutsKV[value.name] = key
                    }

                    mProgress.visibility = View.GONE
                    formContainer.visibility = View.VISIBLE

                    val spinnerArrayAdapter =
                        ArrayAdapter(
                            this@AddSourceActivity,
                            android.R.layout.simple_spinner_item,
                            itemsStrings
                        )
                    spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spoutsSpinner.adapter = spinnerArrayAdapter
                } else {
                    handleProblemWithSpouts()
                }
            }

            override fun onFailure(call: Call<Map<String, Spout>>, t: Throwable) {
                handleProblemWithSpouts()
            }

            private fun handleProblemWithSpouts() {
                Toast.makeText(
                    this@AddSourceActivity,
                    R.string.cant_get_spouts,
                    Toast.LENGTH_SHORT
                ).show()
                mProgress.visibility = View.GONE
            }
        })
    }

    private fun maybeGetDetailsFromIntentSharing(
        intent: Intent,
        sourceUri: EditText,
        nameInput: EditText
    ) {
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            sourceUri.setText(intent.getStringExtra(Intent.EXTRA_TEXT))
            nameInput.setText(intent.getStringExtra(Intent.EXTRA_TITLE))
        }
    }

    private fun mustLoginToAddSource() {
        Toast.makeText(this, getString(R.string.addStringNoUrl), Toast.LENGTH_SHORT).show()
        val i = Intent(this, LoginActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun handleSaveSource(tags: EditText, title: String, url: String, api: SelfossApi) {

        val sourceDetailsAvailable =
            title.isEmpty() || url.isEmpty() || mSpoutsValue == null || mSpoutsValue!!.isEmpty()

        if (sourceDetailsAvailable) {
            Toast.makeText(this, R.string.form_not_complete, Toast.LENGTH_SHORT).show()
        } else {
            api.createSource(
                title,
                url,
                mSpoutsValue!!,
                tags.text.toString(),
                ""
            ).enqueue(object : Callback<SuccessResponse> {
                override fun onResponse(
                    call: Call<SuccessResponse>,
                    response: Response<SuccessResponse>
                ) {
                    if (response.body() != null && response.body()!!.isSuccess) {
                        finish()
                    } else {
                        Toast.makeText(
                            this@AddSourceActivity,
                            R.string.cant_create_source,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                    Toast.makeText(
                        this@AddSourceActivity,
                        R.string.cant_create_source,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }
}
