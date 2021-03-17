package apps.amine.bou.readerforselfoss

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import apps.amine.bou.readerforselfoss.adapters.SourcesListAdapter
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.Source
import apps.amine.bou.readerforselfoss.databinding.ActivityImageBinding
import apps.amine.bou.readerforselfoss.databinding.ActivitySourcesBinding
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.themes.Toppings
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.network.isNetworkAccessible
import com.ftinc.scoop.Scoop
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SourcesActivity : AppCompatActivity() {

    private lateinit var appColors: AppColors
    private lateinit var binding: ActivitySourcesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        appColors = AppColors(this@SourcesActivity)

        super.onCreate(savedInstanceState)
        binding = ActivitySourcesBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        val scoop = Scoop.getInstance()
        scoop.bind(this, Toppings.PRIMARY.value, binding.toolbar)
        if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scoop.bindStatusBar(this, Toppings.PRIMARY_DARK.value)
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        binding.fab.rippleColor = appColors.colorAccentDark
        binding.fab.backgroundTintList = ColorStateList.valueOf(appColors.colorAccent)
    }

    override fun onStop() {
        super.onStop()
        binding.recyclerView.clearOnScrollListeners()
    }

    override fun onResume() {
        super.onResume()
        val mLayoutManager = LinearLayoutManager(this)

        val settings =
            getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val api = SelfossApi(
            this,
            this@SourcesActivity,
            settings.getBoolean("isSelfSignedCert", false),
            prefs.getString("api_timeout", "-1")!!.toLong()
        )
        var items: ArrayList<Source> = ArrayList()

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = mLayoutManager

        if (this@SourcesActivity.isNetworkAccessible(this@SourcesActivity.findViewById(R.id.recyclerView))) {
            api.sources.enqueue(object : Callback<List<Source>> {
                override fun onResponse(
                    call: Call<List<Source>>,
                    response: Response<List<Source>>
                ) {
                    if (response.body() != null && response.body()!!.isNotEmpty()) {
                        items = response.body() as ArrayList<Source>
                    }
                    val mAdapter = SourcesListAdapter(this@SourcesActivity, items, api)
                    binding.recyclerView.adapter = mAdapter
                    mAdapter.notifyDataSetChanged()
                    if (items.isEmpty()) {
                        Toast.makeText(
                            this@SourcesActivity,
                            R.string.nothing_here,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<List<Source>>, t: Throwable) {
                    Toast.makeText(
                        this@SourcesActivity,
                        R.string.cant_get_sources,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }

        binding.fab.setOnClickListener {
            startActivity(Intent(this@SourcesActivity, AddSourceActivity::class.java))
        }
    }
}
