package apps.amine.bou.readerforselfoss

import android.content.Context
import android.content.SharedPreferences
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
import androidx.fragment.app.Fragment
import androidx.room.Room
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.fragments.ArticleFragment
import apps.amine.bou.readerforselfoss.persistence.database.AppDatabase
import apps.amine.bou.readerforselfoss.persistence.entities.ActionEntity
import apps.amine.bou.readerforselfoss.persistence.migrations.MIGRATION_1_2
import apps.amine.bou.readerforselfoss.persistence.migrations.MIGRATION_2_3
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.themes.Toppings
import apps.amine.bou.readerforselfoss.transformers.DepthPageTransformer
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.maybeHandleSilentException
import apps.amine.bou.readerforselfoss.utils.network.isNetworkAccessible
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
    private lateinit var prefs: SharedPreferences

    private var activeAlignment: Int = 1
    val JUSTIFY = 1
    val ALIGN_LEFT = 2

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

    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reader)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "selfoss-database"
        ).addMigrations(MIGRATION_1_2).addMigrations(MIGRATION_2_3).build()

        val scoop = Scoop.getInstance()
        scoop.bind(this, Toppings.PRIMARY.value, toolBar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scoop.bindStatusBar(this, Toppings.PRIMARY_DARK.value)
        }

        setSupportActionBar(toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val settings =
            getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        editor = prefs.edit()

        debugReadingItems = prefs.getBoolean("read_debug", false)
        userIdentifier = prefs.getString("unique_id", "")
        markOnScroll = prefs.getBoolean("mark_on_scroll", false)
        activeAlignment = prefs.getInt("text_align", JUSTIFY)

        api = SelfossApi(
            this,
            this@ReaderActivity,
            settings.getBoolean("isSelfSignedCert", false),
            prefs.getString("api_timeout", "-1").toLong(),
            prefs.getBoolean("should_log_everything", false)
        )

        if (allItems.isEmpty()) {
            finish()
        }

        currentItem = intent.getIntExtra("currentItem", 0)

        readItem(allItems[currentItem])

        pager.adapter =
                ScreenSlidePagerAdapter(supportFragmentManager, AppColors(this@ReaderActivity))
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
            if (this@ReaderActivity.isNetworkAccessible(this@ReaderActivity.findViewById(R.id.reader_activity_view))) {
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
                                ACRA.getErrorReporter()
                                    .maybeHandleSilentException(t, this@ReaderActivity)
                            }
                        }
                    }
                )
            } else {
                thread {
                    db.actionsDao().insertAllActions(ActionEntity(item.id, true, false, false, false))
                }
            }
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

            container.background = ColorDrawable(
                ContextCompat.getColor(
                    this@ReaderActivity,
                    appColors.colorBackground
                )
            )
        }
    }

    fun alignmentMenu(showJustify: Boolean) {
        toolbarMenu.findItem(R.id.align_left).isVisible = !showJustify
        toolbarMenu.findItem(R.id.align_justify).isVisible = showJustify
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
        if (activeAlignment == JUSTIFY) {
            alignmentMenu(false)
        } else {
            alignmentMenu(true)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun afterSave() {
            allItems[pager.currentItem] =
                    allItems[pager.currentItem].toggleStar()
            notifyAdapter()
            canRemoveFromFavorite()
        }

        fun afterUnsave() {
            allItems[pager.currentItem] = allItems[pager.currentItem].toggleStar()
            notifyAdapter()
            canFavorite()
        }

        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.save -> {
                if (this@ReaderActivity.isNetworkAccessible(null)) {
                    api.starrItem(allItems[pager.currentItem].id)
                        .enqueue(object : Callback<SuccessResponse> {
                            override fun onResponse(
                                call: Call<SuccessResponse>,
                                response: Response<SuccessResponse>
                            ) {
                                afterSave()
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
                } else {
                    thread {
                        db.actionsDao().insertAllActions(ActionEntity(allItems[pager.currentItem].id, false, false, true, false))
                        afterSave()
                    }
                }
            }
            R.id.unsave -> {
                if (this@ReaderActivity.isNetworkAccessible(null)) {
                    api.unstarrItem(allItems[pager.currentItem].id)
                        .enqueue(object : Callback<SuccessResponse> {
                            override fun onResponse(
                                call: Call<SuccessResponse>,
                                response: Response<SuccessResponse>
                            ) {
                                afterUnsave()
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
                } else {
                    thread {
                        db.actionsDao().insertAllActions(ActionEntity(allItems[pager.currentItem].id, false, false, false, true))
                        afterUnsave()
                    }
                }
            }
            R.id.align_left -> {
                editor.putInt("text_align", ALIGN_LEFT)
                editor.apply()
                alignmentMenu(true)
                refreshFragment()
            }
            R.id.align_justify -> {
                editor.putInt("text_align", JUSTIFY)
                editor.apply()
                alignmentMenu(false)
                refreshFragment()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshFragment() {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    companion object {
        var allItems: ArrayList<Item> = ArrayList()
    }
}
