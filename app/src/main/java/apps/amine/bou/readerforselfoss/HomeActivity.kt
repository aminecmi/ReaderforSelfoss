package apps.amine.bou.readerforselfoss

import java.lang.Exception

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.MenuItemCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast

import com.anupcowkur.reservoir.Reservoir
import com.anupcowkur.reservoir.ReservoirGetCallback
import com.anupcowkur.reservoir.ReservoirPutCallback
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.InviteEvent
import com.github.stkent.amplify.prompt.DefaultLayoutPromptView
import com.github.stkent.amplify.tracking.Amplify
import com.google.android.gms.appinvite.AppInviteInvitation
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.reflect.TypeToken
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.materialdrawer.Drawer
import com.mikepenz.materialdrawer.DrawerBuilder
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.model.DividerDrawerItem
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import apps.amine.bou.readerforselfoss.adapters.ItemCardAdapter
import apps.amine.bou.readerforselfoss.adapters.ItemListAdapter
import apps.amine.bou.readerforselfoss.api.selfoss.*
import apps.amine.bou.readerforselfoss.settings.SettingsActivity
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.checkAndDisplayStoreApk
import apps.amine.bou.readerforselfoss.utils.checkApkVersion
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.drawer.CustomUrlPrimaryDrawerItem
import apps.amine.bou.readerforselfoss.utils.longHash
import com.ashokvarma.bottomnavigation.BottomNavigationBar
import com.ashokvarma.bottomnavigation.BottomNavigationItem
import com.ashokvarma.bottomnavigation.TextBadgeItem
import com.ftinc.scoop.Scoop


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
    private var clickBehavior = false
    private var internalBrowser = false
    private var articleViewer = false
    private var shouldBeCardView = false
    private var displayUnreadCount = false
    private var displayAllCount = false
    private var fullHeightCards: Boolean = false
    private var elementsShown: Int = 0
    private var maybeTagFilter: Tag? = null
    private var maybeSourceFilter: Sources? = null
    private var maybeSearchFilter: String? = null

    private lateinit var emptyText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var bottomBar: BottomNavigationBar
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tabNewBadge: TextBadgeItem
    private lateinit var tabArchiveBadge: TextBadgeItem
    private lateinit var tabStarredBadge: TextBadgeItem
    private lateinit var toolbar: Toolbar
    private lateinit var drawer: Drawer
    private lateinit var api: SelfossApi
    private lateinit var customTabActivityHelper: CustomTabActivityHelper
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var sharedPref: SharedPreferences
    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var appColors: AppColors



    data class DrawerData(val tags: List<Tag>?, val sources: List<Sources>?)



    override fun onStart() {
        super.onStart()
        customTabActivityHelper.bindCustomTabsService(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Scoop.getInstance().apply(this)

        setContentView(R.layout.activity_home)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            val promptView: DefaultLayoutPromptView = findViewById(R.id.prompt_view)
            Amplify.getSharedInstance().promptIfReady(promptView)
        }

        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.setDefaults(R.xml.default_remote_config)

        customTabActivityHelper = CustomTabActivityHelper()

        api = SelfossApi(this, this@HomeActivity)
        items = ArrayList()

        appColors =  AppColors(this@HomeActivity)

        handleBottomBar()

        handleDrawer()

        coordinatorLayout = findViewById(R.id.coordLayout)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        recyclerView = findViewById(R.id.my_recycler_view)

        emptyText = findViewById(R.id.emptyText)

        reloadLayoutManager()

        swipeRefreshLayout.setColorSchemeResources(
            R.color.refresh_progress_1,
            R.color.refresh_progress_2,
            R.color.refresh_progress_3)
        swipeRefreshLayout.setOnRefreshListener {
            handleDrawerItems()
            getElementsAccordingToTab()
        }

        val simpleItemTouchCallback =
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

                override fun getSwipeDirs(recyclerView: RecyclerView?, viewHolder: RecyclerView.ViewHolder?): Int =
                    if (elementsShown != UNREAD_SHOWN) 0 else super.getSwipeDirs(recyclerView, viewHolder)

                override fun onMove(recyclerView: RecyclerView,
                                    viewHolder: RecyclerView.ViewHolder,
                                    target: RecyclerView.ViewHolder): Boolean  = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, swipeDir: Int) {
                    try {
                        val i = items[viewHolder.adapterPosition]
                        val position = items.indexOf(i)

                        if (shouldBeCardView) {
                            (recyclerView.adapter as ItemCardAdapter).removeItemAtIndex(position)
                        } else {
                            (recyclerView.adapter as ItemListAdapter).removeItemAtIndex(position)
                        }
                        //tabNew.setBadgeCount(items.size - 1)

                        mayBeEmpty()

                    } catch (e: IndexOutOfBoundsException) {}

                }
            }

        ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(recyclerView)

        this@HomeActivity.checkAndDisplayStoreApk()

    }

    private fun handleBottomBar() {

        bottomBar = findViewById(R.id.bottomBar)

        tabNewBadge = TextBadgeItem().setText("")
        tabArchiveBadge = TextBadgeItem().setText("")
        tabStarredBadge = TextBadgeItem().setText("")

        val tabNew =
            BottomNavigationItem(
                R.drawable.ic_fiber_new_black_24dp,
                getString(R.string.tab_new)
            ).setActiveColor(appColors.accent)
                //.setBadgeItem(tabNewBadge)
        val tabArchive =
            BottomNavigationItem(
                R.drawable.ic_archive_black_24dp,
                getString(R.string.tab_read)
            ).setActiveColor(appColors.dark)
          //      .setBadgeItem(tabArchiveBadge)
        val tabStarred =
            BottomNavigationItem(
                R.drawable.ic_favorite_black_24dp,
                getString(R.string.tab_favs)
            ).setActiveColorResource(R.color.pink)
            //    .setBadgeItem(tabStarredBadge)

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

        handleDrawerItems()

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)

        val settings = getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
        editor = settings.edit()

        if (BuildConfig.GITHUB_VERSION) {
            this@HomeActivity.checkApkVersion(settings, editor, firebaseRemoteConfig)
        }

        handleSharedPrefs()

        getElementsAccordingToTab()
    }

    override fun onStop() {
        super.onStop()
        customTabActivityHelper.unbindCustomTabsService(this)
    }


    private fun handleSharedPrefs() {
        clickBehavior = sharedPref.getBoolean("tab_on_tap", false)
        internalBrowser = sharedPref.getBoolean("prefer_internal_browser", true)
        articleViewer = sharedPref.getBoolean("prefer_article_viewer", true)
        shouldBeCardView = sharedPref.getBoolean("card_view_active", false)
        displayUnreadCount = sharedPref.getBoolean("display_unread_count", true)
        displayAllCount = sharedPref.getBoolean("display_other_count", false)
        fullHeightCards = sharedPref.getBoolean("full_height_cards", false)
    }

    private fun handleDrawer() {

        drawer = DrawerBuilder()
            .withActivity(this)
            .withRootView(R.id.drawer_layout)
            .withToolbar(toolbar)
            .withActionBarDrawerToggle(true)
            .withActionBarDrawerToggleAnimated(true)
            .withShowDrawerOnFirstLaunch(true)
            .withOnDrawerListener(object: Drawer.OnDrawerListener {
                override fun onDrawerSlide(v: View?, p1: Float) {
                    bottomBar.alpha = (1 - p1)
                }

                override fun onDrawerClosed(v: View?) {
                    bottomBar.show()
                }

                override fun onDrawerOpened(v: View?) {
                    bottomBar.hide()
                }

            })
            .build()

        drawer.addStickyFooterItem(
            PrimaryDrawerItem()
                .withName(R.string.action_about)
                .withSelectable(false)
                .withIcon(R.drawable.ic_info_outline)
                .withIconTintingEnabled(true)
                .withOnDrawerItemClickListener { _, _, _ ->
                    LibsBuilder()
                        .withActivityStyle(
                            if (appColors.isDarkTheme)
                                Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
                            else
                                Libs.ActivityStyle.DARK
                        )
                        .withAboutIconShown(true)
                        .withAboutVersionShown(true)
                        .start(this@HomeActivity)
                    false
                })
        drawer.addStickyFooterItem(
            PrimaryDrawerItem()
                .withName(R.string.title_activity_settings)
                .withIcon(R.drawable.ic_settings)
                .withIconTintingEnabled(true)
                .withOnDrawerItemClickListener { _, _, _ ->
                    startActivityForResult(
                        Intent(
                            this@HomeActivity,
                            SettingsActivity::class.java
                        ),
                        MENU_PREFERENCES
                    )
                    false
                }
        )

    }

    private fun handleDrawerItems() {
        fun handleDrawerData(maybeDrawerData: DrawerData?, loadedFromCache: Boolean = false) {
            fun handleTags(maybeTags: List<Tag>?) {
                if (maybeTags == null) {
                    if (loadedFromCache)
                        drawer.addItem(
                            SecondaryDrawerItem()
                                .withName(getString(R.string.drawer_error_loading_tags))
                                .withSelectable(false))
                }
                else {
                    for (tag in maybeTags) {
                        val gd: GradientDrawable = GradientDrawable()
                        gd.setColor(Color.parseColor(tag.color))
                        gd.shape = GradientDrawable.RECTANGLE
                        gd.setSize(30, 30)
                        gd.cornerRadius = 30F
                        drawer.addItem(
                            PrimaryDrawerItem()
                                .withName(tag.tag)
                                .withIdentifier(tag.tag.longHash())
                                .withIcon(gd)
                                .withBadge("${tag.unread}")
                                .withBadgeStyle(
                                    BadgeStyle().withTextColor(Color.WHITE)
                                        .withColor(appColors.accent)
                                )
                                .withOnDrawerItemClickListener { _, _, _ ->
                                    maybeTagFilter = tag
                                    getElementsAccordingToTab()
                                    false
                                }
                        )
                    }
                }

            }

            fun handleSources(maybeSources: List<Sources>?) {
                if (maybeSources == null) {
                    if (loadedFromCache)
                        drawer.addItem(
                            SecondaryDrawerItem()
                                .withName(getString(R.string.drawer_error_loading_sources))
                                .withSelectable(false))
                }
                else
                    for (tag in maybeSources)
                        drawer.addItem(
                            CustomUrlPrimaryDrawerItem()
                                .withName(tag.title)
                                .withIdentifier(tag.id.toLong())
                                .withIcon(tag.getIcon(this@HomeActivity))
                                .withOnDrawerItemClickListener { _, _, _ ->
                                    maybeSourceFilter = tag
                                    getElementsAccordingToTab()
                                    false
                                }
                        )

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
                        .withSelectable(false))
                handleTags(maybeDrawerData.tags)
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


                if (!loadedFromCache)
                    Reservoir.putAsync("drawerData", maybeDrawerData, object : ReservoirPutCallback {
                        override fun onSuccess() {}

                        override fun onFailure(p0: Exception?) {
                        }

                    })
            } else {
                if (!loadedFromCache) {
                    drawer.addItem(
                        PrimaryDrawerItem()
                            .withName(getString(R.string.no_tags_loaded))
                            .withIdentifier(DRAWER_ID_TAGS)
                            .withSelectable(false))
                    drawer.addItem(
                        PrimaryDrawerItem()
                            .withName(getString(R.string.no_sources_loaded))
                            .withIdentifier(DRAWER_ID_SOURCES)
                            .withSelectable(false))
                }
            }

        }

        fun drawerApiCalls(maybeDrawerData: DrawerData?) {
            var tags: List<Tag>? = null
            var sources: List<Sources>?

            fun sourcesApiCall() {
                api.sources.enqueue(object: Callback<List<Sources>> {
                    override fun onResponse(call: Call<List<Sources>>?, response: Response<List<Sources>>) {
                        sources = response.body()
                        val apiDrawerData = DrawerData(tags, sources)
                        if ((maybeDrawerData != null && maybeDrawerData != apiDrawerData) || maybeDrawerData == null)
                            handleDrawerData(apiDrawerData)
                    }

                    override fun onFailure(call: Call<List<Sources>>?, t: Throwable?) {

                    }

                })
            }

            api.tags.enqueue(object: Callback<List<Tag>> {
                override fun onResponse(call: Call<List<Tag>>, response: Response<List<Tag>>) {
                    tags = response.body()
                    sourcesApiCall()
                }

                override fun onFailure(call: Call<List<Tag>>?, t: Throwable?) {
                    sourcesApiCall()
                }

            })
        }

        drawer.addItem(PrimaryDrawerItem().withName(getString(R.string.drawer_loading)).withSelectable(false))

        val resultType = object : TypeToken<DrawerData>() {}.type
        Reservoir.getAsync("drawerData", resultType, object: ReservoirGetCallback<DrawerData> {
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
        val mLayoutManager: RecyclerView.LayoutManager
        if (shouldBeCardView) {
            mLayoutManager = StaggeredGridLayoutManager(calculateNoOfColumns(), StaggeredGridLayoutManager.VERTICAL)
            mLayoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        } else {
            mLayoutManager = GridLayoutManager(this, calculateNoOfColumns())
        }

        recyclerView.layoutManager = mLayoutManager
        recyclerView.setHasFixedSize(true)

        bottomBar.setTabSelectedListener(object: BottomNavigationBar.OnTabSelectedListener {
            override fun onTabUnselected(position: Int) = Unit

            override fun onTabReselected(position: Int) =
                if (shouldBeCardView) {
                    if ((mLayoutManager as StaggeredGridLayoutManager).findFirstCompletelyVisibleItemPositions(null)[0] == 0) {
                        getElementsAccordingToTab()
                    } else {
                        mLayoutManager.scrollToPositionWithOffset(0, 0)
                    }
                } else {
                    if ((mLayoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
                        getElementsAccordingToTab()
                    } else {
                        mLayoutManager.scrollToPositionWithOffset(0, 0)
                    }
                }

            override fun onTabSelected(position: Int) =
                when(position) {
                    0 -> getUnRead()
                    1 -> getRead()
                    2 -> getStarred()
                    else -> Unit
                }

        })
    }

    fun mayBeEmpty() =
        if (items.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }

    private fun getElementsAccordingToTab() =
        when (elementsShown) {
            UNREAD_SHOWN -> getUnRead()
            READ_SHOWN -> getRead()
            FAV_SHOWN -> getStarred()
            else -> getUnRead()
        }

    private fun doCallTo(toastMessage: Int, call: (String?, Long?, String?) -> Call<List<Item>>) {
        fun handleItemsResponse(response: Response<List<Item>>) {
            val didUpdate = (response.body() != items)
            if (response.body() != null) {
                if (response.body() != items) {
                    items = response.body() as ArrayList<Item>
                }
            } else {
                items = ArrayList()
            }
            if (didUpdate)
                handleListResult()
            mayBeEmpty()
            swipeRefreshLayout.isRefreshing = false
        }

        if (!swipeRefreshLayout.isRefreshing)
            swipeRefreshLayout.post { swipeRefreshLayout.isRefreshing = true }

        call(maybeTagFilter?.tag, maybeSourceFilter?.id?.toLong(), maybeSearchFilter)
            .enqueue(object : Callback<List<Item>> {
                override fun onResponse(call: Call<List<Item>>, response: Response<List<Item>>) {
                    handleItemsResponse(response)
                }

                override fun onFailure(call: Call<List<Item>>, t: Throwable) {
                    swipeRefreshLayout.isRefreshing = false
                    Toast.makeText(this@HomeActivity, toastMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun getUnRead() {
        elementsShown = UNREAD_SHOWN
        doCallTo(R.string.cant_get_new_elements){t, id, f -> api.newItems(t, id, f)}
    }

    private fun getRead() {
        elementsShown = READ_SHOWN
        doCallTo(R.string.cant_get_read){t, id, f -> api.readItems(t, id, f)}
    }

    private fun getStarred() {
        elementsShown = FAV_SHOWN
        doCallTo(R.string.cant_get_favs){t, id, f -> api.starredItems(t, id, f)}
    }

    private fun handleListResult() {
        reloadLayoutManager()

        val mAdapter: RecyclerView.Adapter<*>
        if (shouldBeCardView) {
            mAdapter =
                ItemCardAdapter(
                    this,
                    items,
                    api,
                    customTabActivityHelper,
                    internalBrowser,
                    articleViewer,
                    fullHeightCards,
                    appColors)
        } else {
            mAdapter =
                ItemListAdapter(
                    this,
                    items,
                    api,
                    customTabActivityHelper,
                    clickBehavior,
                    internalBrowser,
                    articleViewer)
        }
        recyclerView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        reloadBadges()
    }

    private fun reloadBadges() {
        if (displayUnreadCount || displayAllCount) {
            api.stats.enqueue(object : Callback<Stats> {
                override fun onResponse(call: Call<Stats>, response: Response<Stats>) {
                    if (response.body() != null) {
                    tabStarredBadge.setText(response.body()!!.unread.toString())
                        if (displayAllCount) {
                            tabStarredBadge.setText(response.body()!!.total.toString())
                            tabStarredBadge.setText(response.body()!!.starred.toString())
                        }/* else {
                            tabArchive.removeBadge()
                            tabStarred.removeBadge()
                        }*/
                    }
                }

                override fun onFailure(call: Call<Stats>, t: Throwable) {}
            })
        }/* else {
            tabNew.removeBadge()
            tabArchive.removeBadge()
            tabStarred.removeBadge()
        }*/
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.refresh -> {
                api.update().enqueue(object : Callback<String> {
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        Toast.makeText(this@HomeActivity,
                            R.string.refresh_success_response, Toast.LENGTH_LONG)
                            .show()
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Toast.makeText(this@HomeActivity, R.string.refresh_failer_message, Toast.LENGTH_SHORT).show()
                    }
                })
                Toast.makeText(this, R.string.refresh_in_progress, Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.readAll -> {
                if (elementsShown == UNREAD_SHOWN) {
                    swipeRefreshLayout.isRefreshing = false
                    val ids = items.map { it.id }

                    api.readAll(ids).enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {
                            if (response.body() != null && response.body()!!.isSuccess)
                                Toast.makeText(this@HomeActivity, R.string.all_posts_read, Toast.LENGTH_SHORT).show()
                            else
                                Toast.makeText(this@HomeActivity, R.string.all_posts_not_read, Toast.LENGTH_SHORT).show()

                            swipeRefreshLayout.isRefreshing = false
                        }

                        override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                            Toast.makeText(this@HomeActivity, R.string.all_posts_not_read, Toast.LENGTH_SHORT).show()
                            swipeRefreshLayout.isRefreshing = false
                        }
                    })
                    items = ArrayList()
                    if (items.isEmpty())
                        Toast.makeText(this@HomeActivity, R.string.nothing_here, Toast.LENGTH_SHORT).show()
                    handleListResult()
                }
                return true
            }
            R.id.action_disconnect -> {
                return Config.logoutAndRedirect(this, this@HomeActivity, editor)
            }
            R.id.action_share_the_app -> {
                if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
                    val share = AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
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
}
