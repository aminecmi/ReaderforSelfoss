package apps.amine.bou.readerforselfoss

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.widget.Toast

import com.melnykov.fab.FloatingActionButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import apps.amine.bou.readerforselfoss.adapters.SourcesListAdapter
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.Sources
import com.ftinc.scoop.Scoop


class SourcesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scoop.getInstance().apply(this)
        setContentView(R.layout.activity_sources)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onResume() {
        super.onResume()
        val mFab: FloatingActionButton = findViewById(R.id.fab)
        val mRecyclerView: RecyclerView = findViewById(R.id.activity_sources)
        val mLayoutManager = LinearLayoutManager(this)
        val api = SelfossApi(this, this@SourcesActivity)
        var items: ArrayList<Sources> = ArrayList()

        mFab.attachToRecyclerView(mRecyclerView)
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutManager = mLayoutManager

        api.sources.enqueue(object : Callback<List<Sources>> {
            override fun onResponse(call: Call<List<Sources>>, response: Response<List<Sources>>) {
                if (response.body() != null && response.body()!!.isNotEmpty()) {
                    items = response.body() as ArrayList<Sources>
                }
                val mAdapter = SourcesListAdapter(this@SourcesActivity, items, api)
                mRecyclerView.adapter = mAdapter
                mAdapter.notifyDataSetChanged()
                if (items.isEmpty()) Toast.makeText(this@SourcesActivity, R.string.nothing_here, Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(call: Call<List<Sources>>, t: Throwable) {
                Toast.makeText(this@SourcesActivity, R.string.cant_get_sources, Toast.LENGTH_SHORT).show()
            }
        })

        mFab.setOnClickListener {
            startActivity(Intent(this@SourcesActivity, AddSourceActivity::class.java))
        }
    }
}
