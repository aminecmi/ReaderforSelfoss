package apps.amine.bou.readerforselfoss

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.fragments.ArticleFragment
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.themes.Toppings
import apps.amine.bou.readerforselfoss.transformers.DepthPageTransformer
import apps.amine.bou.readerforselfoss.utils.toggleStar
import com.ftinc.scoop.Scoop
import kotlinx.android.synthetic.main.activity_reader.*
import me.relex.circleindicator.CircleIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReaderActivity : AppCompatActivity() {

    private var markOnScroll: Boolean = false
    private var currentItem: Int = 0

    private lateinit var api: SelfossApi

    private lateinit var toolbarMenu: Menu

    private fun showMenuItem(willAddToFavorite: Boolean) {
        toolbarMenu.findItem(R.id.save).isVisible = willAddToFavorite
        toolbarMenu.findItem(R.id.unsave).isVisible = !willAddToFavorite
    }

    private fun canFavorite() {
        showMenuItem(true)
    }

    private fun canRemoveFromFavorite() {
        showMenuItem(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reader)

        val scoop = Scoop.getInstance()
        scoop.bind(this, Toppings.PRIMARY.value, toolBar)
        if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scoop.bindStatusBar(this, Toppings.PRIMARY_DARK.value)
        }

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        api = SelfossApi(
            this,
            this@ReaderActivity,
            prefs.getBoolean("isSelfSignedCert", false),
            prefs.getBoolean("should_log_everything", false)
        )


        if (allItems.isEmpty()) {
            finish()
        }

        currentItem = intent.getIntExtra("currentItem", 0)

        pager.adapter = ScreenSlidePagerAdapter(supportFragmentManager, AppColors(this@ReaderActivity))
        pager.currentItem = currentItem
    }

    override fun onResume() {
        super.onResume()

        notifyAdapter()

        pager.setPageTransformer(true, DepthPageTransformer())
        (indicator as CircleIndicator).setViewPager(pager)
    }

    private fun notifyAdapter() {
        (pager.adapter as ScreenSlidePagerAdapter).notifyDataSetChanged()
    }

    override fun onPause() {
        super.onPause()
        if (markOnScroll) {
            pager.clearOnPageChangeListeners()
        }
    }

    override fun onSaveInstanceState(oldInstanceState: Bundle?) {
        super.onSaveInstanceState(oldInstanceState)
        oldInstanceState!!.clear()
    }

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager, val appColors: AppColors) :
        FragmentStatePagerAdapter(fm) {


        override fun getCount(): Int {
            return allItems.size
        }

        override fun getItem(position: Int): ArticleFragment {
            return ArticleFragment.newInstance(position, allItems)
        }

        override fun startUpdate(container: ViewGroup) {
            super.startUpdate(container)

            container.background = ColorDrawable(ContextCompat.getColor(this@ReaderActivity, appColors.colorBackground))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.reader_menu, menu)
        toolbarMenu = menu

        if (!allItems.isEmpty() && allItems[currentItem].starred) {
            canRemoveFromFavorite()
        } else {
            canFavorite()
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.save -> {
                api.starrItem(allItems[pager.currentItem].id)
                    .enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(
                            call: Call<SuccessResponse>,
                            response: Response<SuccessResponse>
                        ) {
                            allItems[pager.currentItem] = allItems[pager.currentItem].toggleStar()
                            notifyAdapter()
                            canRemoveFromFavorite()
                        }

                        override fun onFailure(
                            call: Call<SuccessResponse>,
                            t: Throwable
                        ) {
                            Toast.makeText(
                                baseContext,
                                R.string.cant_mark_favortie,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
            R.id.unsave -> {
                api.unstarrItem(allItems[pager.currentItem].id)
                    .enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(
                            call: Call<SuccessResponse>,
                            response: Response<SuccessResponse>
                        ) {
                            allItems[pager.currentItem] = allItems[pager.currentItem].toggleStar()
                            notifyAdapter()
                            canFavorite()
                        }

                        override fun onFailure(
                            call: Call<SuccessResponse>,
                            t: Throwable
                        ) {
                            Toast.makeText(
                                baseContext,
                                R.string.cant_unmark_favortie,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        var allItems: ArrayList<Item> = ArrayList()
    }
}
