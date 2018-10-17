package apps.amine.bou.readerforselfoss

import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toast
import androidx.room.Room
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.fragments.ArticleFragment
import apps.amine.bou.readerforselfoss.persistence.database.AppDatabase
import apps.amine.bou.readerforselfoss.persistence.migrations.MIGRATION_1_2
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.themes.Toppings
import apps.amine.bou.readerforselfoss.transformers.DepthPageTransformer
import apps.amine.bou.readerforselfoss.utils.maybeHandleSilentException
import apps.amine.bou.readerforselfoss.utils.persistence.toEntity
import apps.amine.bou.readerforselfoss.utils.succeeded
import apps.amine.bou.readerforselfoss.utils.toggleStar
import com.ftinc.scoop.Scoop
import kotlinx.android.synthetic.main.activity_reader.*
import me.relex.circleindicator.CircleIndicator
import org.acra.ACRA
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class ReaderActivity : AppCompatActivity() {

    private var markOnScroll: Boolean = false
    private var debugReadingItems: Boolean = false
    private var currentItem: Int = 0
    private lateinit var userIdentifier: String

    private lateinit var api: SelfossApi

    private lateinit var toolbarMenu: Menu

    private lateinit var db: AppDatabase

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

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "selfoss-database"
        ).addMigrations(MIGRATION_1_2).build()

        val scoop = Scoop.getInstance()
        scoop.bind(this, Toppings.PRIMARY.value, toolBar)
        if  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scoop.bindStatusBar(this, Toppings.PRIMARY_DARK.value)
        }

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        debugReadingItems = prefs.getBoolean("read_debug", false)
        userIdentifier = prefs.getString("unique_id", "")
        markOnScroll = prefs.getBoolean("mark_on_scroll", false)

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

        readItem(allItems[currentItem])

        pager.adapter = ScreenSlidePagerAdapter(supportFragmentManager, AppColors(this@ReaderActivity))
        pager.currentItem = currentItem
    }

    override fun onResume() {
        super.onResume()

        notifyAdapter()

        pager.setPageTransformer(true, DepthPageTransformer())
        (indicator as CircleIndicator).setViewPager(pager)

        pager.addOnPageChangeListener(
            object : ViewPager.SimpleOnPageChangeListener() {

                override fun onPageSelected(position: Int) {

                    if (allItems[position].starred) {
                        canRemoveFromFavorite()
                    } else {
                        canFavorite()
                    }
                    readItem(allItems[pager.currentItem])
                }
            }
        )
    }

    fun readItem(item: Item) {
        if (markOnScroll) {
            thread {
                db.itemsDao().delete(item.toEntity())
            }
            api.markItem(item.id).enqueue(
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
                            ACRA.getErrorReporter()
                                .maybeHandleSilentException(Exception(message), this@ReaderActivity)
                        }
                    }

                    override fun onFailure(
                        call: Call<SuccessResponse>,
                        t: Throwable
                    ) {
                        thread {
                            db.itemsDao().insertAllItems(item.toEntity())
                        }
                        if (debugReadingItems) {
                            ACRA.getErrorReporter().maybeHandleSilentException(t, this@ReaderActivity)
                        }
                    }
                }
            )
        }
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
