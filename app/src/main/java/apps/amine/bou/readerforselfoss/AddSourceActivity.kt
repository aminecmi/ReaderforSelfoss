package apps.amine.bou.readerforselfoss

import android.content.Intent
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.*

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.Spout
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.isBaseUrlValid
import com.ftinc.scoop.Scoop


class AddSourceActivity : AppCompatActivity() {

    private var mSpoutsValue: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scoop.getInstance().apply(this)
        setContentView(R.layout.activity_add_source)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val mProgress: ProgressBar = findViewById(R.id.progress)
        val mForm: ConstraintLayout = findViewById(R.id.formContainer)
        val mNameInput: EditText = findViewById(R.id.nameInput)
        val mSourceUri: EditText = findViewById(R.id.sourceUri)
        val mTags: EditText = findViewById(R.id.tags)
        val mSpoutsSpinner: Spinner = findViewById(R.id.spoutsSpinner)
        val mSaveBtn: Button = findViewById(R.id.saveBtn)
        var api: SelfossApi? = null

        try {
            api = SelfossApi(this, this@AddSourceActivity)
        } catch (e: IllegalArgumentException) {
            mustLoginToAddSource()
        }

        val intent = intent
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            mSourceUri.setText(intent.getStringExtra(Intent.EXTRA_TEXT))
            mNameInput.setText(intent.getStringExtra(Intent.EXTRA_TITLE))
        }

        mSaveBtn.setOnClickListener {
            handleSaveSource(mTags, mNameInput.text.toString(), mSourceUri.text.toString(), api!!)
        }


        val spoutsKV = HashMap<String, String>()
        mSpoutsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>, view: View, i: Int, l: Long) {
                val spoutName = (view as TextView).text.toString()
                mSpoutsValue = spoutsKV[spoutName]
            }

            override fun onNothingSelected(adapterView: AdapterView<*>) {
                mSpoutsValue = null
            }
        }

        val config = Config(this)

        if (config.baseUrl.isEmpty() || !config.baseUrl.isBaseUrlValid()) {
            mustLoginToAddSource()
        } else {

            var items: Map<String, Spout>
            api!!.spouts().enqueue(object : Callback<Map<String, Spout>> {
                override fun onResponse(call: Call<Map<String, Spout>>, response: Response<Map<String, Spout>>) {
                    if (response.body() != null) {
                        items = response.body()!!

                        val itemsStrings = items.map { it.value.name }
                        for ((key, value) in items) {
                            spoutsKV.put(value.name, key)
                        }

                        mProgress.visibility = View.GONE
                        mForm.visibility = View.VISIBLE

                        val spinnerArrayAdapter =
                            ArrayAdapter(
                                this@AddSourceActivity,
                                android.R.layout.simple_spinner_item,
                                itemsStrings)
                        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        mSpoutsSpinner.adapter = spinnerArrayAdapter

                    } else {
                        handleProblemWithSpouts()
                    }
                }

                override fun onFailure(call: Call<Map<String, Spout>>, t: Throwable) {
                    handleProblemWithSpouts()
                }

                private fun handleProblemWithSpouts() {
                    Toast.makeText(this@AddSourceActivity, R.string.cant_get_spouts, Toast.LENGTH_SHORT).show()
                    mProgress.visibility = View.GONE
                }
            })
        }
    }

    private fun mustLoginToAddSource() {
        Toast.makeText(this, getString(R.string.addStringNoUrl), Toast.LENGTH_SHORT).show()
        val i = Intent(this, LoginActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun handleSaveSource(mTags: EditText, title: String, url: String, api: SelfossApi) {

        if (title.isEmpty() || url.isEmpty() || mSpoutsValue == null || mSpoutsValue!!.isEmpty()) {
            Toast.makeText(this, R.string.form_not_complete, Toast.LENGTH_SHORT).show()
        } else {
            api.createSource(
                title,
                url,
                mSpoutsValue!!,
                mTags.text.toString(),
                ""
            ).enqueue(object : Callback<SuccessResponse> {
                override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {
                    if (response.body() != null && response.body()!!.isSuccess) {
                        finish()
                    } else {
                        Toast.makeText(this@AddSourceActivity, R.string.cant_create_source, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                    Toast.makeText(this@AddSourceActivity, R.string.cant_create_source, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
