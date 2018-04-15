package apps.amine.bou.readerforselfoss

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.widget.Toast
import apps.amine.bou.readerforselfoss.adapters.SourcesListAdapter
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.Sources
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.themes.Toppings
import com.ftinc.scoop.Scoop
import kotlinx.android.synthetic.main.activity_sources.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SourcesActivity : AppCompatActivity() {

    private lateinit var appColors: AppColors

    override fun onCreate(savedInstanceState: Bundle?) {
        appColors = AppColors(this@SourcesActivity)

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_sources)

        Scoop.getInstance()
            .bind(this, Toppings.PRIMARY.value, toolbar)
            .bindStatusBar(this, Toppings.PRIMARY_DARK.value)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        fab.rippleColor = appColors.colorAccentDark
        fab.backgroundTintList = ColorStateList.valueOf(appColors.colorAccent)
    }

    override fun onStop() {
        super.onStop()
        recyclerView.clearOnScrollListeners()
    }

    override fun onResume() {
        super.onResume()
        val mLayoutManager = LinearLayoutManager(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val api = SelfossApi(
            this,
            this@SourcesActivity,
            prefs.getBoolean("isSelfSignedCert", false),
            prefs.getBoolean("should_log_everything", false)
        )
        var items: ArrayList<Sources> = ArrayList()

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = mLayoutManager

        api.sources.enqueue(object : Callback<List<Sources>> {
            override fun onResponse(
                call: Call<List<Sources>>,
                response: Response<List<Sources>>
            ) {
                if (response.body() != null && response.body()!!.isNotEmpty()) {
                    items = response.body() as ArrayList<Sources>
                }
                val mAdapter = SourcesListAdapter(this@SourcesActivity, items, api)
                recyclerView.adapter = mAdapter
                mAdapter.notifyDataSetChanged()
                if (items.isEmpty()) {
                    Toast.makeText(
                        this@SourcesActivity,
                        R.string.nothing_here,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<List<Sources>>, t: Throwable) {
                Toast.makeText(
                    this@SourcesActivity,
                    R.string.cant_get_sources,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        fab.setOnClickListener {
            startActivity(Intent(this@SourcesActivity, AddSourceActivity::class.java))
        }
    }
}
