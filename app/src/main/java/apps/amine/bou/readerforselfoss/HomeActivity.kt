package apps.amine.bou.readerforselfoss

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.core.view.MenuItemCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.room.Room
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import apps.amine.bou.readerforselfoss.adapters.ItemCardAdapter
import apps.amine.bou.readerforselfoss.adapters.ItemListAdapter
import apps.amine.bou.readerforselfoss.adapters.ItemsAdapter
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.Source
import apps.amine.bou.readerforselfoss.api.selfoss.Stats
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.api.selfoss.Tag
import apps.amine.bou.readerforselfoss.background.LoadingWorker
import apps.amine.bou.readerforselfoss.persistence.database.AppDatabase
import apps.amine.bou.readerforselfoss.persistence.entities.ActionEntity
import apps.amine.bou.readerforselfoss.persistence.migrations.MIGRATION_1_2
import apps.amine.bou.readerforselfoss.persistence.migrations.MIGRATION_2_3
import apps.amine.bou.readerforselfoss.settings.SettingsActivity
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.themes.Toppings
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.bottombar.maybeShow
import apps.amine.bou.readerforselfoss.utils.bottombar.removeBadge
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.drawer.CustomUrlPrimaryDrawerItem
import apps.amine.bou.readerforselfoss.utils.flattenTags
import apps.amine.bou.readerforselfoss.utils.longHash
import apps.amine.bou.readerforselfoss.utils.maybeHandleSilentException
import apps.amine.bou.readerforselfoss.utils.network.isNetworkAccessible
import apps.amine.bou.readerforselfoss.utils.persistence.toEntity
import apps.amine.bou.readerforselfoss.utils.persistence.toView
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.profile.profile
import com.ashokvarma.bottomnavigation.BottomNavigationBar
import com.ashokvarma.bottomnavigation.BottomNavigationItem
import com.ashokvarma.bottomnavigation.TextBadgeItem
import com.ftinc.scoop.Scoop
import com.github.stkent.amplify.tracking.Amplify
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import kotlinx.android.synthetic.main.activity_home.*
import org.acra.ACRA
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class HomeActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private val MENU_PREFERENCES = 12302
    private val DRAWER_ID_TAGS = 100101L
    private val DRAWER_ID_HIDDEN_TAGS = 101100L
    private val DRAWER_ID_SOURCES = 100110L
    private val DRAWER_ID_FILTERS = 100111L
    private val UNREAD_SHOWN = 1
    private val READ_SHOWN = 2
    private val FAV_SHOWN = 3

    private var items: ArrayList<Item> = ArrayList()
    private var allItems: ArrayList<Item> = ArrayList()

    private var debugReadingItems = false
    private var shouldLogEverything = false
    private var internalBrowser = false
    private var articleViewer = false
    private var shouldBeCardView = false
    private var displayUnreadCount = false
    private var displayAllCount = false
    private var fullHeightCards: Boolean = false
    private var itemsNumber: Int = 200
    private var elementsShown: Int = 0
    private var maybeTagFilter: Tag? = null
    private var maybeSourceFilter: Source? = null
    private var maybeSearchFilter: String? = null
    private var userIdentifier: String = ""
    private var displayAccountHeader: Boolean = false
    private var infiniteScroll: Boolean = false
    private var lastFetchDone: Boolean = false
    private var itemsCaching: Boolean = false
    private var hiddenTags: List<String> = emptyList()

    private var periodicRefresh = false
    private var refreshMinutes: Long = 360L
    private var refreshWhenChargingOnly = false

    private lateinit var tabNewBadge: TextBadgeItem
    private lateinit var tabArchiveBadge: TextBadgeItem
    private lateinit var tabStarredBadge: TextBadgeItem
    private lateinit var drawer: Drawer
    private lateinit var api: SelfossApi
    private lateinit var customTabActivityHelper: CustomTabActivityHelper
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sharedPref: SharedPreferences
    private lateinit var appColors: AppColors
    private var offset: Int = 0
    private var firstVisible: Int = 0
    private lateinit var recyclerViewScrollListener: RecyclerView.OnScrollListener
    private lateinit var settings: SharedPreferences

    private var recyclerAdapter: RecyclerView.Adapter<*>? = null

    private var badgeNew: Int = -1
    private var badgeAll: Int = -1
    private var badgeFavs: Int = -1

    private var fromTabShortcut: Boolean = false
    private var offlineShortcut: Boolean = false

    private lateinit var tagsBadge: Map<Long, Int>

    private lateinit var db: AppDatabase

    private lateinit var config: Config

    data class DrawerData(val tags: List<Tag>?, val sources: List<Source>?)

    override fun onStart() {
        super.onStart()
        customTabActivityHelper.bindCustomTabsService(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appColors = AppColors(this@HomeActivity)
        config = Config(this@HomeActivity)

        super.onCreate(savedInstanceState)

        fromTabShortcut =  intent.getIntExtra("shortcutTab", -1) != -1
        offlineShortcut =  intent.getBooleanExtra("startOffline", false)

        if (fromTabShortcut) {
            elementsShown = intent.getIntExtra("shortcutTab", UNREAD_SHOWN)
        }

        setContentView(R.layout.activity_home)

        handleThemeBinding()

        setSupportActionBar(toolBar)
        if (savedInstanceState == null) {
            Amplify.getSharedInstance().promptIfReady(promptView)
        }

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "selfoss-database"
        ).addMigrations(MIGRATION_1_2).addMigrations(MIGRATION_2_3).build()


        customTabActivityHelper = CustomTabActivityHelper()

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        settings = getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)

        api = SelfossApi(
            this,
            this@HomeActivity,
            settings.getBoolean("isSelfSignedCert", false),
            sharedPref.getString("api_timeout", "-1").toLong(),
            shouldLogEverything
        )
        items = ArrayList()
        allItems = ArrayList()

        handleBottomBar()
        handleDrawer()

        handleSwipeRefreshLayout()
    }

    private fun handleSwipeRefreshLayout() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.refresh_progress_1,
            R.color.refresh_progress_2,
            R.color.refresh_progress_3
        )
        swipeRefreshLayout.setOnRefreshListener {
            offlineShortcut = false
            allItems = ArrayList()
            lastFetchDone = false
            handleDrawerItems()
            getElementsAccordingToTab()
        }

        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun getSwipeDirs(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ): Int =
                    if (elementsShown != UNREAD_SHOWN && elementsShown != READ_SHOWN) {
                        0
                    } else {
                        super.getSwipeDirs(
                            recyclerView,
                            viewHolder
                        )
                    }

                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                    val position = viewHolder.adapterPosition
                    val i = items.elementAtOrNull(position)

                    if (i != null) {
                        val adapter = recyclerView.adapter as ItemsAdapter<*>

                        val wasItemUnread = adapter.unreadItemStatusAtIndex(position)

                        adapter.handleItemAtIndex(position)

                        if (wasItemUnread) {
                            badgeNew--
                        } else {
                            badgeNew++
                        }

                        reloadBadgeContent()

                        val tagHashes = i.tags.tags.split(",").map { it.longHash() }
                        tagsBadge = tagsBadge.map {
                            if (tagHashes.contains(it.key)) {
                                (it.key to (it.value - 1))
                            } else {
                                (it.key to it.value)
                            }
                        }.toMap()
                        reloadTagsBadges()

                        // Just load everythin
                        if (items.size <= 0) {
                            getElementsAccordingToTab()
                        }
                    } else {
                        Toast.makeText(
                            this@HomeActivity,
                            "Found null when swiping at positon $position.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(recyclerView)
    }

    private fun handleBottomBar() {

        tabNewBadge = TextBadgeItem()
            .setText("")
            .setHideOnSelect(false).hide(false)
            .setBackgroundColor(appColors.colorPrimary)
        tabArchiveBadge = TextBadgeItem()
            .setText("")
            .setHideOnSelect(false).hide(false)
            .setBackgroundColor(appColors.colorPrimary)
        tabStarredBadge = TextBadgeItem()
            .setText("")
            .setHideOnSelect(false).hide(false)
            .setBackgroundColor(appColors.colorPrimary)

        val tabNew =
            BottomNavigationItem(
                R.drawable.ic_tab_fiber_new_black_24dp,
                getString(R.string.tab_new)
            ).setActiveColor(appColors.colorAccent)
                .setBadgeItem(tabNewBadge)
        val tabArchive =
            BottomNavigationItem(
                R.drawable.ic_tab_archive_black_24dp,
                getString(R.string.tab_read)
            ).setActiveColor(appColors.colorAccentDark)
                .setBadgeItem(tabArchiveBadge)
        val tabStarred =
            BottomNavigationItem(
                R.drawable.ic_tab_favorite_black_24dp,
                getString(R.string.tab_favs)
            ).setActiveColorResource(R.color.pink)
                .setBadgeItem(tabStarredBadge)

        bottomBar
            .addItem(tabNew)
            .addItem(tabArchive)
            .addItem(tabStarred)
            .setFirstSelectedPosition(0)
            .initialise()

        bottomBar.setMode(BottomNavigationBar.MODE_SHIFTING)
        bottomBar.setBackgroundStyle(BottomNavigationBar.BACKGROUND_STYLE_STATIC)

        if (fromTabShortcut) {
            bottomBar.selectTab(elementsShown - 1)
        }
    }

    override fun onResume() {
        super.onResume()

        // TODO: Make this the only appcolors init
        appColors = AppColors(this@HomeActivity)

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        editor = settings.edit()

        handleSharedPrefs()

        handleDrawerItems()

        handleThemeUpdate()

        reloadLayoutManager()

        if (!infiniteScroll) {
            recyclerView.setHasFixedSize(true)
        } else {
            handleInfiniteScroll()
        }

        handleBottomBarActions()

        getElementsAccordingToTab()

        handleGDPRDialog(sharedPref.getBoolean("GDPR_shown", false))

        handleRecurringTask()

        handleOfflineActions()
    }

    private fun getAndStoreAllItems() {
        api.allItems().enqueue(object : Callback<List<Item>> {
            override fun onFailure(call: Call<List<Item>>, t: Throwable) {
            }

            override fun onResponse(
                call: Call<List<Item>>,
                response: Response<List<Item>>
            ) {
                thread {
                    if (response.body() != null) {
                        val apiItems = (response.body() as ArrayList<Item>).filter {
                            maybeTagFilter != null || filter(it.tags.tags)
                        } as ArrayList<Item>
                        db.itemsDao().deleteAllItems()
                        db.itemsDao()
                            .insertAllItems(*(apiItems.map { it.toEntity() }).toTypedArray())
                    }
                }
            }
        })
    }

    override fun onStop() {
        super.onStop()
        customTabActivityHelper.unbindCustomTabsService(this)
    }

    private fun handleSharedPrefs() {
        debugReadingItems = sharedPref.getBoolean("read_debug", false)
        shouldLogEverything = sharedPref.getBoolean("should_log_everything", false)
        internalBrowser = sharedPref.getBoolean("prefer_internal_browser", true)
        articleViewer = sharedPref.getBoolean("prefer_article_viewer", true)
        shouldBeCardView = sharedPref.getBoolean("card_view_active", false)
        displayUnreadCount = sharedPref.getBoolean("display_unread_count", true)
        displayAllCount = sharedPref.getBoolean("display_other_count", false)
        fullHeightCards = sharedPref.getBoolean("full_height_cards", false)
        itemsNumber = sharedPref.getString("prefer_api_items_number", "200").toInt()
        userIdentifier = sharedPref.getString("unique_id", "")
        displayAccountHeader = sharedPref.getBoolean("account_header_displaying", false)
        infiniteScroll = sharedPref.getBoolean("infinite_loading", false)
        itemsCaching = sharedPref.getBoolean("items_caching", false)
        hiddenTags = if (sharedPref.getString("hidden_tags", "").isNotEmpty()) {
            sharedPref.getString("hidden_tags", "").replace("\\s".toRegex(), "").split(",")
        } else {
            emptyList()
        }
        periodicRefresh = sharedPref.getBoolean("periodic_refresh", false)
        refreshWhenChargingOnly = sharedPref.getBoolean("refresh_when_charging", false)
        refreshMinutes = sharedPref.getString("periodic_refresh_minutes", "360").toLong()

        if (refreshMinutes <= 15) {
            refreshMinutes = 15
        }
    }

    private fun handleThemeBinding() {
        val scoop = Scoop.getInstance()
        scoop.bind(this, Toppings.PRIMARY.value, toolBar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scoop.bindStatusBar(this, Toppings.PRIMARY_DARK.value)
        }
    }

    private fun handleThemeUpdate() {

        val scoop = Scoop.getInstance()
        scoop.update(Toppings.PRIMARY.value, appColors.colorPrimary)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            scoop.update(Toppings.PRIMARY_DARK.value, appColors.colorPrimaryDark)
        }
    }

    private fun handleDrawer() {
        displayAccountHeader =
                PreferenceManager.getDefaultSharedPreferences(this)
                    .getBoolean("account_header_displaying", false)

        drawer = drawer {
            rootViewRes = R.id.drawer_layout
            toolbar = toolBar
            actionBarDrawerToggleEnabled = true
            actionBarDrawerToggleAnimated = true
            showOnFirstLaunch = true
            onSlide { _, p1 ->
                bottomBar.alpha = (1 - p1)
            }
            onClosed {
                bottomBar.show()
            }
            onOpened {
                bottomBar.hide()
            }

            if (displayAccountHeader) {
                accountHeader {
                    background = R.drawable.bg
                    profile(settings.getString("url", "")) {
                        iconDrawable = resources.getDrawable(R.mipmap.ic_launcher)
                    }
                    selectionListEnabledForSingleProfile = false
                }
            }

            footer {
                primaryItem(R.string.drawer_report_bug) {
                    icon = R.drawable.ic_bug_report_black_24dp
                    iconTintingEnabled = true
                    onClick { _ ->
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Config.trackerUrl))
                        startActivity(browserIntent)
                        false
                    }
                }

                primaryItem(R.string.title_activity_settings) {
                    icon = R.drawable.ic_settings_black_24dp
                    iconTintingEnabled = true
                    onClick { _ ->
                        startActivityForResult(
                            Intent(
                                this@HomeActivity,
                                SettingsActivity::class.java
                            ),
                            MENU_PREFERENCES
                        )
                        false
                    }
                }
            }
        }
    }

    private fun handleDrawerItems() {
        tagsBadge = emptyMap()
        fun handleDrawerData(maybeDrawerData: DrawerData?, loadedFromCache: Boolean = false) {
            fun handleTags(maybeTags: List<Tag>?) {
                if (maybeTags == null) {
                    if (loadedFromCache) {
                        drawer.addItem(
                            SecondaryDrawerItem()
                                .withName(getString(R.string.drawer_error_loading_tags))
                                .withSelectable(false)
                        )
                    }
                } else {
                    val filteredTags = maybeTags
                        .filterNot { hiddenTags.contains(it.tag) }
                        .sortedBy { it.unread == 0 }
                    tagsBadge = filteredTags.map {
                        val gd = GradientDrawable()
                        val color = try {
                            Color.parseColor(it.color)
                        } catch (e: IllegalArgumentException) {
                            appColors.colorPrimary
                        }

                        gd.setColor(color)
                        gd.shape = GradientDrawable.RECTANGLE
                        gd.setSize(30, 30)
                        gd.cornerRadius = 30F
                        var drawerItem =
                            PrimaryDrawerItem()
                                .withName(it.tag)
                                .withIdentifier(it.tag.longHash())
                                .withIcon(gd)
                                .withBadgeStyle(
                                    BadgeStyle().withTextColor(Color.WHITE)
                                        .withColor(appColors.colorAccent)
                                )
                                .withOnDrawerItemClickListener { _, _, _ ->
                                    allItems = ArrayList()
                                    maybeTagFilter = it
                                    getElementsAccordingToTab()
                                    false
                                }
                        if (it.unread > 0) {
                            drawerItem = drawerItem.withBadge("${it.unread}")
                        }
                        drawer.addItem(
                            drawerItem
                        )

                        (it.tag.longHash() to it.unread)
                    }.toMap()
                }
            }

            fun handleHiddenTags(maybeTags: List<Tag>?) {
                if (maybeTags == null) {
                    if (loadedFromCache) {
                        drawer.addItem(
                            SecondaryDrawerItem()
                                .withName(getString(R.string.drawer_error_loading_tags))
                                .withSelectable(false)
                        )
                    }
                } else {
                    val filteredHiddenTags: List<Tag> =
                        maybeTags.filter { hiddenTags.contains(it.tag) }
                    tagsBadge = filteredHiddenTags.map {
                        val gd = GradientDrawable()
                        val color = try {
                            Color.parseColor(it.color)
                        } catch (e: IllegalArgumentException) {
                            appColors.colorPrimary
                        }

                        gd.setColor(color)
                        gd.shape = GradientDrawable.RECTANGLE
                        gd.setSize(30, 30)
                        gd.cornerRadius = 30F
                        var drawerItem =
                            PrimaryDrawerItem()
                                .withName(it.tag)
                                .withIdentifier(it.tag.longHash())
                                .withIcon(gd)
                                .withBadgeStyle(
                                    BadgeStyle().withTextColor(Color.WHITE)
                                        .withColor(appColors.colorAccent)
                                )
                                .withOnDrawerItemClickListener { _, _, _ ->
                                    allItems = ArrayList()
                                    maybeTagFilter = it
                                    getElementsAccordingToTab()
                                    false
                                }
                        if (it.unread > 0) {
                            drawerItem = drawerItem.withBadge("${it.unread}")
                        }
                        drawer.addItem(
                            drawerItem
                        )

                        (it.tag.longHash() to it.unread)
                    }.toMap()
                }
            }

            fun handleSources(maybeSources: List<Source>?) {
                if (maybeSources == null) {
                    if (loadedFromCache) {
                        drawer.addItem(
                            SecondaryDrawerItem()
                                .withName(getString(R.string.drawer_error_loading_sources))
                                .withSelectable(false)
                        )
                    }
                } else {
                    for (tag in maybeSources) {
                        drawer.addItem(
                            CustomUrlPrimaryDrawerItem()
                                .withName(tag.title)
                                .withIdentifier(tag.id.toLong())
                                .withIcon(tag.getIcon(this@HomeActivity))
                                .withOnDrawerItemClickListener { _, _, _ ->
                                    allItems = ArrayList()
                                    maybeSourceFilter = tag
                                    getElementsAccordingToTab()
                                    false
                                }
                        )
                    }
                }
            }

            drawer.removeAllItems()
            if (maybeDrawerData != null) {
                drawer.addItem(
                    SecondaryDrawerItem()
                        .withName(getString(R.string.drawer_item_filters))
                        .withSelectable(false)
                        .withIdentifier(DRAWER_ID_FILTERS)
                        .withBadge(getString(R.string.drawer_action_clear))
                        .withOnDrawerItemClickListener { _, _, _ ->
                            allItems = ArrayList()
                            maybeSourceFilter = null
                            maybeTagFilter = null
                            getElementsAccordingToTab()
                            false
                        }
                )
                if (hiddenTags.isNotEmpty()) {
                    drawer.addItem(DividerDrawerItem())
                    drawer.addItem(
                        SecondaryDrawerItem()
                            .withName(getString(R.string.drawer_item_hidden_tags))
                            .withIdentifier(DRAWER_ID_HIDDEN_TAGS)
                            .withSelectable(false)
                    )
                    handleHiddenTags(maybeDrawerData.tags)
                }
                drawer.addItem(DividerDrawerItem())
                drawer.addItem(
                    SecondaryDrawerItem()
                        .withName(getString(R.string.drawer_item_tags))
                        .withIdentifier(DRAWER_ID_TAGS)
                        .withSelectable(false)
                )
                handleTags(maybeDrawerData.tags)
                drawer.addItem(DividerDrawerItem())
                drawer.addItem(
                    SecondaryDrawerItem()
                        .withName(getString(R.string.drawer_item_sources))
                        .withIdentifier(DRAWER_ID_TAGS)
                        .withBadge(getString(R.string.drawer_action_edit))
                        .withSelectable(false)
                        .withOnDrawerItemClickListener { _, _, _ ->
                            startActivity(Intent(this, SourcesActivity::class.java))
                            false
                        }
                )
                handleSources(maybeDrawerData.sources)
                drawer.addItem(DividerDrawerItem())
                drawer.addItem(
                    PrimaryDrawerItem()
                        .withName(R.string.action_about)
                        .withSelectable(false)
                        .withIcon(R.drawable.ic_info_outline_white_24dp)
                        .withIconTintingEnabled(true)
                        .withOnDrawerItemClickListener { _, _, _ ->
                            LibsBuilder()
                                .withActivityStyle(
                                    if (appColors.isDarkTheme) {
                                        Libs.ActivityStyle.DARK
                                    } else {
                                        Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
                                    }
                                )
                                .withAboutIconShown(true)
                                .withAboutVersionShown(true)
                                .start(this@HomeActivity)
                            false
                        }
                )


                if (!loadedFromCache) {
                    if (maybeDrawerData.tags != null) {
                        thread {
                            val tagEntities = maybeDrawerData.tags.map { it.toEntity() }
                            db.drawerDataDao().insertAllTags(*tagEntities.toTypedArray())
                        }
                    }
                    if (maybeDrawerData.sources != null) {
                        thread {
                            val sourceEntities =
                                maybeDrawerData.sources.map { it.toEntity() }
                            db.drawerDataDao().insertAllSources(*sourceEntities.toTypedArray())
                        }
                    }
                }
            } else {
                if (!loadedFromCache) {
                    drawer.addItem(
                        PrimaryDrawerItem()
                            .withName(getString(R.string.no_tags_loaded))
                            .withIdentifier(DRAWER_ID_TAGS)
                            .withSelectable(false)
                    )
                    drawer.addItem(
                        PrimaryDrawerItem()
                            .withName(getString(R.string.no_sources_loaded))
                            .withIdentifier(DRAWER_ID_SOURCES)
                            .withSelectable(false)
                    )
                }
            }
        }

        fun drawerApiCalls(maybeDrawerData: DrawerData?) {
            var tags: List<Tag>? = null
            var sources: List<Source>?

            fun sourcesApiCall() {
                if (this@HomeActivity.isNetworkAccessible(null, offlineShortcut)) {
                    api.sources.enqueue(object : Callback<List<Source>> {
                        override fun onResponse(
                            call: Call<List<Source>>?,
                            response: Response<List<Source>>
                        ) {
                            sources = response.body()
                            val apiDrawerData = DrawerData(tags, sources)
                            if ((maybeDrawerData != null && maybeDrawerData != apiDrawerData) || maybeDrawerData == null) {
                                handleDrawerData(apiDrawerData)
                            }
                        }

                        override fun onFailure(call: Call<List<Source>>?, t: Throwable?) {
                            val apiDrawerData = DrawerData(tags, null)
                            if ((maybeDrawerData != null && maybeDrawerData != apiDrawerData) || maybeDrawerData == null) {
                                handleDrawerData(apiDrawerData)
                            }
                        }
                    })
                }
            }

            if (this@HomeActivity.isNetworkAccessible(null, offlineShortcut)) {
                api.tags.enqueue(object : Callback<List<Tag>> {
                    override fun onResponse(
                        call: Call<List<Tag>>,
                        response: Response<List<Tag>>
                    ) {
                        tags = response.body()
                        sourcesApiCall()
                    }

                    override fun onFailure(call: Call<List<Tag>>?, t: Throwable?) {
                        sourcesApiCall()
                    }
                })
            }
        }

        drawer.addItem(
            PrimaryDrawerItem().withName(getString(R.string.drawer_loading)).withSelectable(
                false
            )
        )

        thread {
            var drawerData = DrawerData(db.drawerDataDao().tags().map { it.toView() },
                                        db.drawerDataDao().sources().map { it.toView() })
            runOnUiThread {
                handleDrawerData(drawerData, loadedFromCache = true)
                drawerApiCalls(drawerData)
            }
        }
    }

    private fun reloadLayoutManager() {
        val currentManager = recyclerView.layoutManager
        val layoutManager: RecyclerView.LayoutManager

        // This will only update the layout manager if settings changed
        when (currentManager) {
            is StaggeredGridLayoutManager ->
                if (!shouldBeCardView) {
                    layoutManager = GridLayoutManager(
                        this,
                        calculateNoOfColumns()
                    )
                    recyclerView.layoutManager = layoutManager
                }
            is GridLayoutManager ->
                if (shouldBeCardView) {
                    layoutManager = StaggeredGridLayoutManager(
                        calculateNoOfColumns(),
                        StaggeredGridLayoutManager.VERTICAL
                    )
                    layoutManager.gapStrategy =
                            StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
                    recyclerView.layoutManager = layoutManager
                }
            else ->
                if (currentManager == null) {
                    if (!shouldBeCardView) {
                        layoutManager = GridLayoutManager(
                            this,
                            calculateNoOfColumns()
                        )
                        recyclerView.layoutManager = layoutManager
                    } else {
                        layoutManager = StaggeredGridLayoutManager(
                            calculateNoOfColumns(),
                            StaggeredGridLayoutManager.VERTICAL
                        )
                        layoutManager.gapStrategy =
                                StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
                        recyclerView.layoutManager = layoutManager
                    }
                } else {
                }
        }
    }

    private fun handleBottomBarActions() {
        bottomBar.setTabSelectedListener(object : BottomNavigationBar.OnTabSelectedListener {
            override fun onTabUnselected(position: Int) = Unit

            override fun onTabReselected(position: Int) {
                val layoutManager = recyclerView.adapter

                when (layoutManager) {
                    is StaggeredGridLayoutManager ->
                        if (layoutManager.findFirstCompletelyVisibleItemPositions(null)[0] == 0) {
                            getElementsAccordingToTab()
                        } else {
                            layoutManager.scrollToPositionWithOffset(0, 0)
                        }
                    is GridLayoutManager ->
                        if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                            getElementsAccordingToTab()
                        } else {
                            layoutManager.scrollToPositionWithOffset(0, 0)
                        }
                    else -> Unit
                }
            }

            override fun onTabSelected(position: Int) {
                offset = 0
                lastFetchDone = false

                if (itemsCaching) {

                    if (!swipeRefreshLayout.isRefreshing) {
                        swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = true }
                    }

                    thread {
                        val dbItems = db.itemsDao().items().map { it.toView() }
                        runOnUiThread {
                            if (dbItems.isNotEmpty()) {
                                items = when (position) {
                                    0 -> ArrayList(dbItems.filter { it.unread })
                                    1 -> ArrayList(dbItems.filter { !it.unread })
                                    2 -> ArrayList(dbItems.filter { it.starred })
                                    else -> ArrayList(dbItems.filter { it.unread })
                                }
                                handleListResult()
                                when (position) {
                                    0 -> getUnRead()
                                    1 -> getRead()
                                    2 -> getStarred()
                                    else -> Unit
                                }
                            } else {
                                if (this@HomeActivity.isNetworkAccessible(this@HomeActivity.findViewById(R.id.coordLayout), offlineShortcut)) {
                                    when (position) {
                                        0 -> getUnRead()
                                        1 -> getRead()
                                        2 -> getStarred()
                                        else -> Unit
                                    }
                                    getAndStoreAllItems()
                                }
                            }
                        }
                    }

                } else {
                    when (position) {
                        0 -> getUnRead()
                        1 -> getRead()
                        2 -> getStarred()
                        else -> Unit
                    }
                }
            }
        })
    }

    private fun handleInfiniteScroll() {
        recyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(localRecycler: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    val manager = recyclerView.layoutManager
                    val lastVisibleItem: Int = when (manager) {
                        is StaggeredGridLayoutManager -> manager.findLastCompletelyVisibleItemPositions(
                            null
                        ).last()
                        is GridLayoutManager -> manager.findLastCompletelyVisibleItemPosition()
                        else -> 0
                    }

                    if (lastVisibleItem == (items.size - 1) && items.size < maxItemNumber()) {
                        getElementsAccordingToTab(appendResults = true)
                    }
                }
            }
        }

        recyclerView.clearOnScrollListeners()
        recyclerView.addOnScrollListener(recyclerViewScrollListener)
    }

    private fun mayBeEmpty() =
        if (items.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

    private fun getElementsAccordingToTab(
        appendResults: Boolean = false,
        offsetOverride: Int? = null
    ) {
        fun doGetAccordingToTab() {
            when (elementsShown) {
                UNREAD_SHOWN -> getUnRead(appendResults)
                READ_SHOWN -> getRead(appendResults)
                FAV_SHOWN -> getStarred(appendResults)
                else -> getUnRead(appendResults)
            }
        }

        offset = if (appendResults && offsetOverride === null) {
            (offset + itemsNumber)
        } else {
            offsetOverride ?: 0
        }
        firstVisible = if (appendResults) firstVisible else 0

        if (itemsCaching) {

            if (!swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = true }
            }

            thread {
                val dbItems = db.itemsDao().items().map { it.toView() }
                runOnUiThread {
                    if (dbItems.isNotEmpty()) {
                        items = when (elementsShown) {
                            UNREAD_SHOWN -> ArrayList(dbItems.filter { it.unread })
                            READ_SHOWN -> ArrayList(dbItems.filter { !it.unread })
                            FAV_SHOWN -> ArrayList(dbItems.filter { it.starred })
                            else -> ArrayList(dbItems.filter { it.unread })
                        }
                        handleListResult()
                        doGetAccordingToTab()
                    } else {
                        if (this@HomeActivity.isNetworkAccessible(this@HomeActivity.findViewById(R.id.coordLayout), offlineShortcut)) {
                            doGetAccordingToTab()
                            getAndStoreAllItems()
                        }
                    }
                }
            }

        } else {
            doGetAccordingToTab()
        }

    }

    private fun filter(tags: String): Boolean {
        val tagsList = tags.replace("\\s".toRegex(), "").split(",")
        return tagsList.intersect(hiddenTags).isEmpty()
    }

    private fun doCallTo(
        appendResults: Boolean,
        toastMessage: Int,
        call: (String?, Long?, String?) -> Call<List<Item>>
    ) {
        fun handleItemsResponse(response: Response<List<Item>>) {
            val shouldUpdate = (response.body()?.toSet() != items.toSet())
            if (response.body() != null) {
                if (shouldUpdate) {
                    getAndStoreAllItems()
                    items = response.body() as ArrayList<Item>
                    items = items.filter {
                        maybeTagFilter != null || filter(it.tags.tags)
                    } as ArrayList<Item>

                    if (allItems.isEmpty()) {
                        allItems = items
                    } else {
                        items.forEach {
                            if (!allItems.contains(it)) allItems.add(it)
                        }
                    }
                }
            } else {
                if (!appendResults) {
                    items = ArrayList()
                    allItems = ArrayList()
                }
            }

            handleListResult(appendResults)

            if (!appendResults) mayBeEmpty()
            swipeRefreshLayout.isRefreshing = false
        }

        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = true }
        }

        if (this@HomeActivity.isNetworkAccessible(this@HomeActivity.findViewById(R.id.coordLayout), offlineShortcut)) {
            call(maybeTagFilter?.tag, maybeSourceFilter?.id?.toLong(), maybeSearchFilter)
                .enqueue(object : Callback<List<Item>> {
                    override fun onResponse(
                        call: Call<List<Item>>,
                        response: Response<List<Item>>
                    ) {
                        handleItemsResponse(response)
                    }

                    override fun onFailure(call: Call<List<Item>>, t: Throwable) {
                        swipeRefreshLayout.isRefreshing = false
                        Toast.makeText(
                            this@HomeActivity,
                            toastMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        } else {
            swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = false }
        }
    }

    private fun getUnRead(appendResults: Boolean = false) {
        elementsShown = UNREAD_SHOWN
        doCallTo(appendResults, R.string.cant_get_new_elements) { t, id, f ->
            api.newItems(
                t,
                id,
                f,
                itemsNumber,
                offset
            )
        }
    }

    private fun getRead(appendResults: Boolean = false) {
        elementsShown = READ_SHOWN
        doCallTo(appendResults, R.string.cant_get_read) { t, id, f ->
            api.readItems(
                t,
                id,
                f,
                itemsNumber,
                offset
            )
        }
    }

    private fun getStarred(appendResults: Boolean = false) {
        elementsShown = FAV_SHOWN
        doCallTo(appendResults, R.string.cant_get_favs) { t, id, f ->
            api.starredItems(
                t,
                id,
                f,
                itemsNumber,
                offset
            )
        }
    }

    private fun handleListResult(appendResults: Boolean = false) {
        if (appendResults) {
            val oldManager = recyclerView.layoutManager
            firstVisible = when (oldManager) {
                is StaggeredGridLayoutManager ->
                    oldManager.findFirstCompletelyVisibleItemPositions(null).last()
                is GridLayoutManager ->
                    oldManager.findFirstCompletelyVisibleItemPosition()
                else -> 0
            }
        }

        if (recyclerAdapter == null) {
            if (shouldBeCardView) {
                recyclerAdapter =
                        ItemCardAdapter(
                            this,
                            items,
                            api,
                            db,
                            customTabActivityHelper,
                            internalBrowser,
                            articleViewer,
                            fullHeightCards,
                            appColors,
                            debugReadingItems,
                            userIdentifier,
                            config
                        ) {
                            updateItems(it)
                        }
            } else {
                recyclerAdapter =
                        ItemListAdapter(
                            this,
                            items,
                            api,
                            db,
                            customTabActivityHelper,
                            internalBrowser,
                            articleViewer,
                            debugReadingItems,
                            userIdentifier,
                            appColors,
                            config
                        ) {
                            updateItems(it)
                        }

                recyclerView.addItemDecoration(
                    DividerItemDecoration(
                        this@HomeActivity,
                        DividerItemDecoration.VERTICAL
                    )
                )
            }
            recyclerView.adapter = recyclerAdapter
        } else {
            if (!appendResults) {
                (recyclerAdapter as ItemsAdapter<*>).updateAllItems(items)
            } else {
                (recyclerAdapter as ItemsAdapter<*>).addItemsAtEnd(items)
            }
        }

        reloadBadges()
    }

    private fun reloadBadges() {
        if (this@HomeActivity.isNetworkAccessible(null, offlineShortcut) && (displayUnreadCount || displayAllCount)) {
            api.stats.enqueue(object : Callback<Stats> {
                override fun onResponse(call: Call<Stats>, response: Response<Stats>) {
                    if (response.body() != null) {

                        badgeNew = response.body()!!.unread
                        badgeAll = response.body()!!.total
                        badgeFavs = response.body()!!.starred
                        reloadBadgeContent()
                    }
                }

                override fun onFailure(call: Call<Stats>, t: Throwable) {
                }
            })
        } else {
            reloadBadgeContent(succeeded = false)
        }
    }

    private fun reloadBadgeContent(succeeded: Boolean = true) {
        if (succeeded) {
            if (displayUnreadCount) {
                tabNewBadge
                    .setText(badgeNew.toString())
                    .maybeShow()
            }
            if (displayAllCount) {
                tabArchiveBadge
                    .setText(badgeAll.toString())
                    .maybeShow()
                tabStarredBadge
                    .setText(badgeFavs.toString())
                    .maybeShow()
            } else {
                tabArchiveBadge.removeBadge()
                tabStarredBadge.removeBadge()
            }
        } else {
            tabNewBadge.removeBadge()
            tabArchiveBadge.removeBadge()
            tabStarredBadge.removeBadge()
        }
    }

    private fun reloadTagsBadges() {
        tagsBadge.forEach {
            drawer.updateBadge(it.key, StringHolder("${it.value}"))
        }
        drawer.resetDrawerContent()
    }

    private fun calculateNoOfColumns(): Int {
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return (dpWidth / 300).toInt()
    }

    override fun onQueryTextChange(p0: String?): Boolean {
        if (p0.isNullOrBlank()) {
            maybeSearchFilter = null
            getElementsAccordingToTab()
        }
        return false
    }

    override fun onQueryTextSubmit(p0: String?): Boolean {
        maybeSearchFilter = p0
        getElementsAccordingToTab()
        return false
    }

    override fun onActivityResult(req: Int, result: Int, data: Intent?) {
        when (req) {
            MENU_PREFERENCES -> {
                drawer.closeDrawer()
                recreate()
            }
            else -> super.onActivityResult(req, result, data)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.home_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = MenuItemCompat.getActionView(searchItem) as SearchView
        searchView.setOnQueryTextListener(this)

        return true
    }

    private fun needsConfirmation(titleRes: Int, messageRes: Int, doFn: () -> Unit) {
        AlertDialog.Builder(this@HomeActivity)
            .setMessage(messageRes)
            .setTitle(titleRes)
            .setPositiveButton(android.R.string.ok) { _, _ -> doFn() }
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                if (this@HomeActivity.isNetworkAccessible(null, offlineShortcut)) {
                    needsConfirmation(R.string.menu_home_refresh, R.string.refresh_dialog_message) {
                        api.update().enqueue(object : Callback<String> {
                            override fun onResponse(
                                call: Call<String>,
                                response: Response<String>
                            ) {
                                Toast.makeText(
                                    this@HomeActivity,
                                    R.string.refresh_success_response, Toast.LENGTH_LONG
                                )
                                    .show()
                            }

                            override fun onFailure(call: Call<String>, t: Throwable) {
                                Toast.makeText(
                                    this@HomeActivity,
                                    R.string.refresh_failer_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                        Toast.makeText(this, R.string.refresh_in_progress, Toast.LENGTH_SHORT).show()
                    }
                    return true
                } else {
                    return false
                }
            }
            R.id.readAll -> {
                if (elementsShown == UNREAD_SHOWN) {
                    needsConfirmation(R.string.readAll, R.string.markall_dialog_message) {
                        swipeRefreshLayout.isRefreshing = false
                        val ids = allItems.map { it.id }
                        val itemsByTag: Map<Long, Int> =
                            allItems.flattenTags()
                                .groupBy { it.tags.tags.longHash() }
                                .map { it.key to it.value.size }
                                .toMap()

                        fun readAllDebug(e: Throwable) {
                            ACRA.getErrorReporter().maybeHandleSilentException(e, this@HomeActivity)
                        }

                        if (ids.isNotEmpty() && this@HomeActivity.isNetworkAccessible(null, offlineShortcut)) {
                            api.readAll(ids).enqueue(object : Callback<SuccessResponse> {
                                override fun onResponse(
                                    call: Call<SuccessResponse>,
                                    response: Response<SuccessResponse>
                                ) {
                                    if (response.body() != null && response.body()!!.isSuccess) {
                                        Toast.makeText(
                                            this@HomeActivity,
                                            R.string.all_posts_read,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        tabNewBadge.removeBadge()

                                        handleDrawerItems()

                                        getElementsAccordingToTab()
                                    } else {
                                        Toast.makeText(
                                            this@HomeActivity,
                                            R.string.all_posts_not_read,
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        if (debugReadingItems) {
                                            readAllDebug(
                                                Throwable(
                                                    "Got response, but : response.body() (${response.body()}) != null && response.body()!!.isSuccess (${response.body()?.isSuccess})." +
                                                            "Request url was (${call.request().url()}), ids were $ids"
                                                )
                                            )
                                        }
                                    }

                                    swipeRefreshLayout.isRefreshing = false
                                }

                                override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                                    Toast.makeText(
                                        this@HomeActivity,
                                        R.string.all_posts_not_read,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    swipeRefreshLayout.isRefreshing = false

                                    if (debugReadingItems) {
                                        readAllDebug(t)
                                    }
                                }
                            })
                            items = ArrayList()
                            allItems = ArrayList()
                        }
                        if (items.isEmpty()) {
                            Toast.makeText(
                                this@HomeActivity,
                                R.string.nothing_here,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        handleListResult()
                    }
                }
                return true
            }
            R.id.action_disconnect -> {
                return Config.logoutAndRedirect(this, this@HomeActivity, editor)
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun maxItemNumber(): Int =
        when (elementsShown) {
            UNREAD_SHOWN -> badgeNew
            READ_SHOWN -> badgeAll
            FAV_SHOWN -> badgeFavs
            else -> badgeNew // if !elementsShown then unread are fetched.
        }

    private fun updateItems(adapterItems: ArrayList<Item>) {
        items = adapterItems
    }

    private fun handleGDPRDialog(GDPRShown: Boolean) {
        val sharedEditor = sharedPref.edit()
        if (!GDPRShown) {
            val alertDialog = AlertDialog.Builder(this).create()
            alertDialog.setTitle(getString(R.string.gdpr_dialog_title))
            alertDialog.setMessage(getString(R.string.gdpr_dialog_message))
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL,
                "OK"
            ) { dialog, _ ->
                sharedEditor.putBoolean("GDPR_shown", true)
                sharedEditor.commit()
                dialog.dismiss()
            }
            alertDialog.show()
        }
    }

    private fun handleRecurringTask() {
        if (periodicRefresh) {
            val myConstraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresCharging(refreshWhenChargingOnly)
                .setRequiresStorageNotLow(true)
                .build()

            val backgroundWork =
                PeriodicWorkRequestBuilder<LoadingWorker>(refreshMinutes, TimeUnit.MINUTES)
                    .setConstraints(myConstraints)
                    .addTag("selfoss-loading")
                    .build()


            WorkManager.getInstance().enqueueUniquePeriodicWork("selfoss-loading", ExistingPeriodicWorkPolicy.KEEP, backgroundWork)
        }
    }

    private fun handleOfflineActions() {
        fun <T>doAndReportOnFail(call: Call<T>, action: ActionEntity) {
           call.enqueue(object: Callback<T> {
               override fun onResponse(
                   call: Call<T>,
                   response: Response<T>
               ) {
                   thread {
                       db.actionsDao().delete(action)
                   }
               }

               override fun onFailure(call: Call<T>, t: Throwable) {
                   ACRA.getErrorReporter().maybeHandleSilentException(t, this@HomeActivity)
               }
           })
        }

        if (this@HomeActivity.isNetworkAccessible(null, offlineShortcut)) {
            thread {
                val actions = db.actionsDao().actions()

                actions.forEach { action ->
                    when {
                        action.read -> doAndReportOnFail(api.markItem(action.articleId), action)
                        action.unread -> doAndReportOnFail(api.unmarkItem(action.articleId), action)
                        action.starred -> doAndReportOnFail(api.starrItem(action.articleId), action)
                        action.unstarred -> doAndReportOnFail(api.unstarrItem(action.articleId), action)
                    }
                }
            }
        }
    }
}

