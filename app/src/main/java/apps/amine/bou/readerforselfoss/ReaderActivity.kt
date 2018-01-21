package apps.amine.bou.readerforselfoss

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.fragments.ArticleFragment
import apps.amine.bou.readerforselfoss.transformers.DepthPageTransformer
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.succeeded
import apps.amine.bou.readerforselfoss.utils.toggleStar
import com.crashlytics.android.Crashlytics
import com.ftinc.scoop.Scoop
import kotlinx.android.synthetic.main.activity_reader.*
import me.relex.circleindicator.CircleIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReaderActivity : AppCompatActivity() {

    private var markOnScroll: Boolean = false
    private var debugReadingItems: Boolean = false
    private var currentItem: Int = 0
    private lateinit var userIdentifier: String

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
        Scoop.getInstance().apply(this)
        setContentView(R.layout.activity_reader)

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val settings = getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        debugReadingItems = sharedPref.getBoolean("read_debug", false)
        userIdentifier = sharedPref.getString("unique_id", "")
        markOnScroll = sharedPref.getBoolean("mark_on_scroll", false)

        if (allItems.isEmpty()) {
            Crashlytics.setUserIdentifier(userIdentifier)
            Crashlytics.log(
                100,
                "READER_ITEMS_EMPTY",
                "Items empty when trying to open the Article Reader. Was (static) companion object field set ?"
            )
            Crashlytics.logException(Exception("Empty items on Reader Activity."))

            finish()
        }

        api = SelfossApi(
            this,
            this@ReaderActivity,
            settings.getBoolean("isSelfSignedCert", false),
            sharedPref.getBoolean("should_log_everything", false)
        )

        currentItem = intent.getIntExtra("currentItem", 0)

        var adapter = ScreenSlidePagerAdapter(supportFragmentManager)
        pager.adapter = adapter
        pager.currentItem = currentItem
    }

    override fun onResume() {
        super.onResume()

        (pager.adapter as ScreenSlidePagerAdapter).notifyDataSetChanged()

        pager.setPageTransformer(true, DepthPageTransformer())
        (indicator as CircleIndicator).setViewPager(pager)

        pager.addOnPageChangeListener(
            object : ViewPager.SimpleOnPageChangeListener() {
                var isLastItem = false

                override fun onPageSelected(position: Int) {
                    isLastItem = (position === (allItems.size - 1))

                    if (allItems[position].starred) {
                        canRemoveFromFavorite()
                    } else {
                        canFavorite()
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {
                    if (markOnScroll && (state === ViewPager.SCROLL_STATE_DRAGGING || (state === ViewPager.SCROLL_STATE_IDLE && isLastItem))) {
                        api.markItem(allItems[pager.currentItem].id).enqueue(
                            object : Callback<SuccessResponse> {
                                override fun onResponse(
                                    call: Call<SuccessResponse>,
                                    response: Response<SuccessResponse>
                                ) {
                                    if (!response.succeeded() && debugReadingItems) {
                                        val message =
                                            "message: ${response.message()} " +
                                                    "response isSuccess: ${response.isSuccessful} " +
                                                    "response code: ${response.code()} " +
                                                    "response message: ${response.message()} " +
                                                    "response errorBody: ${response.errorBody()?.string()} " +
                                                    "body success: ${response.body()?.success} " +
                                                    "body isSuccess: ${response.body()?.isSuccess}"
                                        Crashlytics.setUserIdentifier(userIdentifier)
                                        Crashlytics.log(
                                            100,
                                            "READ_DEBUG_SUCCESS",
                                            message
                                        )
                                        Crashlytics.logException(Exception("Was success, but did it work ?"))
                                    }
                                }

                                override fun onFailure(
                                    call: Call<SuccessResponse>,
                                    t: Throwable
                                ) {
                                    if (debugReadingItems) {
                                        Crashlytics.setUserIdentifier(userIdentifier)
                                        Crashlytics.log(
                                            100,
                                            "READ_DEBUG_ERROR",
                                            t.message
                                        )
                                        Crashlytics.logException(t)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        )
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

    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm) {
        override fun getCount(): Int {
            return allItems.size
        }

        override fun getItem(position: Int): ArticleFragment {
            return ArticleFragment.newInstance(position, allItems)
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
