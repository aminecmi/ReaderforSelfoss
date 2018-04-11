package apps.amine.bou.readerforselfoss

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import apps.amine.bou.readerforselfoss.adapters.ItemCardAdapter
import apps.amine.bou.readerforselfoss.adapters.ItemListAdapter
import apps.amine.bou.readerforselfoss.adapters.ItemsAdapter
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.Sources
import apps.amine.bou.readerforselfoss.api.selfoss.Stats
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.api.selfoss.Tag
import apps.amine.bou.readerforselfoss.settings.SettingsActivity
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.themes.Toppings
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.bottombar.maybeShow
import apps.amine.bou.readerforselfoss.utils.bottombar.removeBadge
import apps.amine.bou.readerforselfoss.utils.checkApkVersion
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.drawer.CustomUrlPrimaryDrawerItem
import apps.amine.bou.readerforselfoss.utils.flattenTags
import apps.amine.bou.readerforselfoss.utils.longHash
import co.zsmb.materialdrawerkt.builders.accountHeader
import co.zsmb.materialdrawerkt.builders.drawer
import co.zsmb.materialdrawerkt.builders.footer
import co.zsmb.materialdrawerkt.draweritems.badgeable.primaryItem
import co.zsmb.materialdrawerkt.draweritems.profile.profile
import com.anupcowkur.reservoir.Reservoir
import com.anupcowkur.reservoir.ReservoirGetCallback
import com.anupcowkur.reservoir.ReservoirPutCallback
import com.ashokvarma.bottomnavigation.BottomNavigationBar
import com.ashokvarma.bottomnavigation.BottomNavigationItem
import com.ashokvarma.bottomnavigation.TextBadgeItem
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.InviteEvent
import com.ftinc.scoop.Scoop
import com.github.stkent.amplify.tracking.Amplify
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.reflect.TypeToken
import com.heinrichreimersoftware.androidissuereporter.IssueReporterLauncher
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import kotlinx.android.synthetic.main.activity_home.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private val MENU_PREFERENCES = 12302
    private val REQUEST_INVITE = 13231
    private val REQUEST_INVITE_BYMAIL = 13232
    private val DRAWER_ID_TAGS = 100101L
    private val DRAWER_ID_SOURCES = 100110L
    private val DRAWER_ID_FILTERS = 100111L
    private val UNREAD_SHOWN = 1
    private val READ_SHOWN = 2
    private val FAV_SHOWN = 3

    private var items: ArrayList<Item> = ArrayList()
    private var allItems: ArrayList<Item> = ArrayList()

    private var clickBehavior = false
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
    private var maybeSourceFilter: Sources? = null
    private var maybeSearchFilter: String? = null
    private var userIdentifier: String = ""
    private var displayAccountHeader: Boolean = false
    private var infiniteScroll: Boolean = false
    private var lastFetchDone: Boolean = false


    private lateinit var tabNewBadge: TextBadgeItem
    private lateinit var tabArchiveBadge: TextBadgeItem
    private lateinit var tabStarredBadge: TextBadgeItem
    private lateinit var drawer: Drawer
    private lateinit var api: SelfossApi
    private lateinit var customTabActivityHelper: CustomTabActivityHelper
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sharedPref: SharedPreferences
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var appColors: AppColors
    private var offset: Int = 0
    private var firstVisible: Int = 0
    private var recyclerViewScrollListener: RecyclerView.OnScrollListener? = null
    private lateinit var settings: SharedPreferences

    private var recyclerAdapter: RecyclerView.Adapter<*>? = null

    private var badgeNew: Int = -1
    private var badgeAll: Int = -1
    private var badgeFavs: Int = -1


    private lateinit var tagsBadge: Map<Long, Int>

    data class DrawerData(val tags: List<Tag>?, val sources: List<Sources>?)

    override fun onStart() {
        super.onStart()
        customTabActivityHelper.bindCustomTabsService(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appColors = AppColors(this@HomeActivity)

        setContentView(R.layout.activity_home)

        handleThemeBinding()

        setSupportActionBar(toolBar)
        if (savedInstanceState == null) {
            Amplify.getSharedInstance().promptIfReady(promptView)
        }

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.setDefaults(R.xml.default_remote_config)

        customTabActivityHelper = CustomTabActivityHelper()

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        settings = getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)

        api = SelfossApi(
            this,
            this@HomeActivity,
            settings.getBoolean("isSelfSignedCert", false),
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
            handleDrawerItems()
            getElementsAccordingToTab()
        }

        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
            ) {
                override fun getSwipeDirs(
                    recyclerView: RecyclerView?,
                    viewHolder: RecyclerView.ViewHolder?
                ): Int =
                    if (elementsShown != UNREAD_SHOWN) {
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
                        val adapter = recyclerView.adapter

                        when (adapter) {
                            is ItemCardAdapter -> adapter.removeItemAtIndex(position)
                            is ItemListAdapter -> adapter.removeItemAtIndex(position)
                        }

                        if (items.size > 0) {
                            badgeNew--
                            reloadBadgeContent()

                            val tagHashes = i.tags.split(",").map { it.longHash() }
                            tagsBadge = tagsBadge.map {
                                if (tagHashes.contains(it.key)) {
                                    (it.key to (it.value - 1))
                                } else {
                                    (it.key to it.value)
                                }
                            }.toMap()
                            reloadTagsBadges()
                        } else {
                            tabNewBadge.hide()
                        }

                        val manager = recyclerView.layoutManager
                        val lastVisibleItem: Int = when (manager) {
                            is StaggeredGridLayoutManager -> manager.findLastCompletelyVisibleItemPositions(
                                null
                            ).last()
                            is GridLayoutManager -> manager.findLastCompletelyVisibleItemPosition()
                            else -> 0
                        }

                        if (lastVisibleItem === items.size &&
                            items.size <= maxItemNumber() &&
                            (maxItemNumber() >= itemsNumber || !lastFetchDone)
                        ) {
                            if (maxItemNumber() < itemsNumber) {
                                lastFetchDone = true
                            }
                            getElementsAccordingToTab(
                                appendResults = true,
                                offsetOverride = lastVisibleItem
                            )
                        }
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
                R.drawable.ic_fiber_new_black_24dp,
                getString(R.string.tab_new)
            ).setActiveColor(appColors.colorAccent)
                .setBadgeItem(tabNewBadge)
        val tabArchive =
            BottomNavigationItem(
                R.drawable.ic_archive_black_24dp,
                getString(R.string.tab_read)
            ).setActiveColor(appColors.colorAccentDark)
                .setBadgeItem(tabArchiveBadge)
        val tabStarred =
            BottomNavigationItem(
                R.drawable.ic_favorite_black_24dp,
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
    }

    override fun onResume() {
        super.onResume()

        // TODO: Make this the only appcolors init
        appColors = AppColors(this@HomeActivity)

        handleDrawerItems()

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        editor = settings.edit()

        if (BuildConfig.GITHUB_VERSION) {
            this@HomeActivity.checkApkVersion(settings, editor, firebaseRemoteConfig)
        }

        handleSharedPrefs()

        handleThemeUpdate()

        reloadLayoutManager()

        if (!infiniteScroll) {
            recyclerView.setHasFixedSize(true)
        } else {
            handleInfiniteScroll()
        }

        handleBottomBarActions()

        getElementsAccordingToTab()
    }

    override fun onStop() {
        super.onStop()
        customTabActivityHelper.unbindCustomTabsService(this)
    }

    private fun handleSharedPrefs() {
        debugReadingItems = sharedPref.getBoolean("read_debug", false)
        shouldLogEverything = sharedPref.getBoolean("should_log_everything", false)
        clickBehavior = sharedPref.getBoolean("tab_on_tap", false)
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
    }

    private fun handleThemeBinding() {
        Scoop.getInstance()
            .bind(this, Toppings.PRIMARY.value, toolBar)
            .bindStatusBar(this, Toppings.PRIMARY_DARK.value)
    }

    private fun handleThemeUpdate() {

        Scoop.getInstance()
            .update(Toppings.PRIMARY.value, appColors.colorPrimary)
            .update(Toppings.PRIMARY_DARK.value, appColors.colorPrimaryDark)
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
                    icon = R.drawable.ic_bug_report
                    iconTintingEnabled = true
                    onClick { _ ->
                        IssueReporterLauncher.forTarget(
                            getString(R.string.report_github_user),
                            getString(R.string.report_github_repo)
                        )
                            .theme(R.style.Theme_App_Light)
                            .guestToken(BuildConfig.GITHUB_TOKEN)
                            .guestEmailRequired(true)
                            .minDescriptionLength(20)
                            .putExtraInfo("Unique ID", settings.getString("unique_id", ""))
                            .putExtraInfo("From github", BuildConfig.GITHUB_VERSION)
                            .homeAsUpEnabled(true)
                            .launch(this@HomeActivity)
                        false
                    }
                }

                primaryItem(R.string.title_activity_settings) {
                    icon = R.drawable.ic_settings
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
                    tagsBadge = maybeTags.map {
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
                        drawer.addItem(
                            PrimaryDrawerItem()
                                .withName(it.tag)
                                .withIdentifier(it.tag.longHash())
                                .withIcon(gd)
                                .withBadge("${it.unread}")
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
                        )

                        (it.tag.longHash() to it.unread)
                    }.toMap()
                }
            }

            fun handleSources(maybeSources: List<Sources>?) {
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
                        .withIcon(R.drawable.ic_info_outline)
                        .withIconTintingEnabled(true)
                        .withOnDrawerItemClickListener { _, _, _ ->
                            LibsBuilder()
                                .withActivityStyle(
                                    if (appColors.isDarkTheme) {
                                        Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
                                    } else {
                                        Libs.ActivityStyle.DARK
                                    }
                                )
                                .withAboutIconShown(true)
                                .withAboutVersionShown(true)
                                .start(this@HomeActivity)
                            false
                        }
                )


                if (!loadedFromCache) {
                    Reservoir.putAsync(
                        "drawerData", maybeDrawerData, object : ReservoirPutCallback {
                            override fun onSuccess() {
                            }

                            override fun onFailure(p0: Exception?) {
                            }
                        })
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
            var sources: List<Sources>?

            fun sourcesApiCall() {
                api.sources.enqueue(object : Callback<List<Sources>> {
                    override fun onResponse(
                        call: Call<List<Sources>>?,
                        response: Response<List<Sources>>
                    ) {
                        sources = response.body()
                        val apiDrawerData = DrawerData(tags, sources)
                        if ((maybeDrawerData != null && maybeDrawerData != apiDrawerData) || maybeDrawerData == null) {
                            handleDrawerData(apiDrawerData)
                        }
                    }

                    override fun onFailure(call: Call<List<Sources>>?, t: Throwable?) {
                    }
                })
            }

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

        drawer.addItem(
            PrimaryDrawerItem().withName(getString(R.string.drawer_loading)).withSelectable(
                false
            )
        )

        val resultType = object : TypeToken<DrawerData>() {}.type
        Reservoir.getAsync(
            "drawerData", resultType, object : ReservoirGetCallback<DrawerData> {
                override fun onSuccess(maybeDrawerData: DrawerData?) {
                    handleDrawerData(maybeDrawerData, loadedFromCache = true)
                    drawerApiCalls(maybeDrawerData)
                }

                override fun onFailure(p0: Exception?) {
                    drawerApiCalls(null)
                }
            })
    }

    private fun reloadLayoutManager() {
        val currentManager = recyclerView.layoutManager
        val layoutManager: RecyclerView.LayoutManager

        // This will only update the layout manager if settings changed
        when (currentManager) {
            is StaggeredGridLayoutManager ->
                if (!shouldBeCardView) {
                    layoutManager = GridLayoutManager(this, calculateNoOfColumns())
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
                        layoutManager = GridLayoutManager(this, calculateNoOfColumns())
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
                when (position) {
                    0 -> getUnRead()
                    1 -> getRead()
                    2 -> getStarred()
                    else -> Unit
                }
            }
        })
    }

    private fun handleInfiniteScroll() {
        if (recyclerViewScrollListener == null) {
            recyclerViewScrollListener = object : RecyclerView.OnScrollListener() {
                override fun onScrolled(localRecycler: RecyclerView?, dx: Int, dy: Int) {
                    if (localRecycler != null && dy > 0) {
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
        offset = if (appendResults && offsetOverride === null) {
            (offset + itemsNumber)
        } else {
            offsetOverride ?: 0
        }
        firstVisible = if (appendResults) firstVisible else 0

        when (elementsShown) {
            UNREAD_SHOWN -> getUnRead(appendResults)
            READ_SHOWN -> getRead(appendResults)
            FAV_SHOWN -> getStarred(appendResults)
            else -> getUnRead(appendResults)
        }
    }

    private fun doCallTo(
        appendResults: Boolean,
        toastMessage: Int,
        call: (String?, Long?, String?) -> Call<List<Item>>
    ) {
        fun handleItemsResponse(response: Response<List<Item>>) {
            val shouldUpdate = (response.body() != items)
            if (response.body() != null) {
                if (shouldUpdate) {
                    items = response.body() as ArrayList<Item>

                    items.forEach {
                        if (!allItems.contains(it)) allItems.add(it)
                    }
                }
            } else {
                if (!appendResults) {
                    items = ArrayList()
                    allItems = ArrayList()
                }
            }
            if (shouldUpdate) {
                handleListResult(appendResults)
            }

            if (!appendResults) mayBeEmpty()
            swipeRefreshLayout.isRefreshing = false
        }

        if (!swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = true }
        }

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
                            customTabActivityHelper,
                            internalBrowser,
                            articleViewer,
                            fullHeightCards,
                            appColors,
                            debugReadingItems,
                            userIdentifier
                        )
            } else {
                recyclerAdapter =
                        ItemListAdapter(
                            this,
                            items,
                            api,
                            customTabActivityHelper,
                            clickBehavior,
                            internalBrowser,
                            articleViewer,
                            debugReadingItems,
                            userIdentifier,
                            appColors
                        )

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
        if (displayUnreadCount || displayAllCount) {
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
            REQUEST_INVITE -> if (result == Activity.RESULT_OK) {
                Answers.getInstance().logInvite(InviteEvent())
            }
            REQUEST_INVITE_BYMAIL -> {
                Answers.getInstance().logInvite(InviteEvent())
                super.onActivityResult(req, result, data)
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
                needsConfirmation(R.string.menu_home_refresh, R.string.refresh_dialog_message, {
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
                })
                return true
            }
            R.id.readAll -> {
                if (elementsShown == UNREAD_SHOWN) {
                    needsConfirmation(R.string.readAll, R.string.markall_dialog_message, {
                        swipeRefreshLayout.isRefreshing = false
                        val ids = allItems.map { it.id }
                        val itemsByTag: Map<Long, Int> =
                            allItems.flattenTags()
                                .groupBy { it.tags.longHash() }
                                .map { it.key to it.value.size }
                                .toMap()

                        fun readAllDebug(e: Throwable) {
                            Crashlytics.setUserIdentifier(userIdentifier)
                            Crashlytics.log(100, "READ_ALL_ERROR", e.message)
                            Crashlytics.logException(e)
                        }

                        if (ids.isNotEmpty()) {
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


                                        tagsBadge = itemsByTag.map {
                                            (it.key to ((tagsBadge[it.key] ?: it.value) - it.value))
                                        }.toMap()

                                        reloadTagsBadges()

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
                    })
                }
                return true
            }
            R.id.action_disconnect -> {
                return Config.logoutAndRedirect(this, this@HomeActivity, editor)
            }
            R.id.action_share_the_app -> {
                if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
                    val share =
                        AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                            .setMessage(getString(R.string.invitation_message))
                            .setDeepLink(Uri.parse("https://ymbh5.app.goo.gl/qbvQ"))
                            .setCallToActionText(getString(R.string.invitation_cta))
                            .build()
                    startActivityForResult(share, REQUEST_INVITE)
                } else {
                    val sendIntent = Intent()
                    sendIntent.action = Intent.ACTION_SEND
                    sendIntent.putExtra(
                        Intent.EXTRA_TEXT,
                        getString(R.string.invitation_message) + " https://ymbh5.app.goo.gl/qbvQ"
                    )
                    sendIntent.type = "text/plain"
                    startActivityForResult(sendIntent, REQUEST_INVITE_BYMAIL)
                }
                return super.onOptionsItemSelected(item)
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
}
