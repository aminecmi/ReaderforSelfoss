package apps.amine.bou.readerforselfoss.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.*
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import android.webkit.WebSettings
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.room.Room
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.mercury.MercuryApi
import apps.amine.bou.readerforselfoss.api.mercury.ParsedContent
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.persistence.database.AppDatabase
import apps.amine.bou.readerforselfoss.persistence.entities.ActionEntity
import apps.amine.bou.readerforselfoss.persistence.migrations.MIGRATION_1_2
import apps.amine.bou.readerforselfoss.persistence.migrations.MIGRATION_2_3
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.buildCustomTabsIntent
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.glide.loadMaybeBasicAuth
import apps.amine.bou.readerforselfoss.utils.isEmptyOrNullOrNullString
import apps.amine.bou.readerforselfoss.utils.network.isNetworkAccessible
import apps.amine.bou.readerforselfoss.utils.openItemUrl
import apps.amine.bou.readerforselfoss.utils.shareLink
import apps.amine.bou.readerforselfoss.utils.sourceAndDateText
import apps.amine.bou.readerforselfoss.utils.succeeded
import android.webkit.WebView
import android.webkit.WebViewClient
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import kotlinx.android.synthetic.main.fragment_article.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.MalformedURLException
import java.net.URL
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class ArticleFragment : Fragment() {
    private lateinit var pageNumber: Number
    private var fontSize: Int = 16
    private lateinit var allItems: ArrayList<Item>
    private var mCustomTabActivityHelper: CustomTabActivityHelper? = null;
    private lateinit var url: String
    private lateinit var contentText: String
    private lateinit var contentSource: String
    private lateinit var contentImage: String
    private lateinit var contentTitle: String
    private lateinit var allImages : ArrayList<String>
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var fab: FloatingActionButton
    private lateinit var appColors: AppColors
    private lateinit var db: AppDatabase
    private lateinit var textAlignment: String
    private lateinit var config: Config

    private var rootView: ViewGroup? = null

    private lateinit var prefs: SharedPreferences

    private var typeface: Typeface? = null
    private var resId: Int = 0
    private var font = ""

    override fun onStop() {
        super.onStop()
        if (mCustomTabActivityHelper != null) {
            mCustomTabActivityHelper!!.unbindCustomTabsService(activity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appColors = AppColors(activity!!)
        config = Config(activity!!)

        super.onCreate(savedInstanceState)

        pageNumber = arguments!!.getInt(ARG_POSITION)
        allItems = arguments!!.getParcelableArrayList(ARG_ITEMS)

        db = Room.databaseBuilder(
            context!!,
            AppDatabase::class.java, "selfoss-database"
        ).addMigrations(MIGRATION_1_2).addMigrations(MIGRATION_2_3).build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        try {
            rootView = inflater
                .inflate(R.layout.fragment_article, container, false) as ViewGroup

            url = allItems[pageNumber.toInt()].getLinkDecoded()
            contentText = allItems[pageNumber.toInt()].content
            contentTitle = allItems[pageNumber.toInt()].getTitleDecoded()
            contentImage = allItems[pageNumber.toInt()].getThumbnail(activity!!)
            contentSource = allItems[pageNumber.toInt()].sourceAndDateText()
            allImages = allItems[pageNumber.toInt()].getImages()

            prefs = PreferenceManager.getDefaultSharedPreferences(activity)
            editor = prefs.edit()
            fontSize = prefs.getString("reader_font_size", "16").toInt()

            font = prefs.getString("reader_font", "")
            if (font.isNotEmpty()) {
                resId = context!!.resources.getIdentifier(font, "font", context!!.packageName)
                typeface = try {
                    ResourcesCompat.getFont(context!!, resId)!!
                } catch (e: java.lang.Exception) {
                    // ACRA.getErrorReporter().maybeHandleSilentException(Throwable("Font loading issue: ${e.message}"), context!!)
                    // Just to be sure
                    null
                }
            }

            refreshAlignment()

            val settings = activity!!.getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)

            val api = SelfossApi(
                context!!,
                activity!!,
                settings.getBoolean("isSelfSignedCert", false),
                prefs.getString("api_timeout", "-1").toLong()
            )

            fab = rootView!!.fab

            fab.backgroundTintList = ColorStateList.valueOf(appColors.colorAccent)

            fab.rippleColor = appColors.colorAccentDark

            val floatingToolbar: FloatingToolbar = rootView!!.floatingToolbar
            floatingToolbar.attachFab(fab)

            floatingToolbar.background = ColorDrawable(appColors.colorAccent)

            val customTabsIntent = activity!!.buildCustomTabsIntent()
            mCustomTabActivityHelper = CustomTabActivityHelper()
            mCustomTabActivityHelper!!.bindCustomTabsService(activity)


            floatingToolbar.setClickListener(
                object : FloatingToolbar.ItemClickListener {
                    override fun onItemClick(item: MenuItem) {
                        when (item.itemId) {
                            R.id.more_action -> getContentFromMercury(customTabsIntent, prefs)
                            R.id.share_action -> activity!!.shareLink(url, contentTitle)
                            R.id.open_action -> activity!!.openItemUrl(
                                allItems,
                                pageNumber.toInt(),
                                url,
                                customTabsIntent,
                                false,
                                false,
                                activity!!
                            )
                            R.id.unread_action -> if ((context != null && context!!.isNetworkAccessible(null)) || context == null) {
                                api.unmarkItem(allItems[pageNumber.toInt()].id).enqueue(
                                    object : Callback<SuccessResponse> {
                                        override fun onResponse(
                                            call: Call<SuccessResponse>,
                                            response: Response<SuccessResponse>
                                        ) {
                                        }

                                        override fun onFailure(
                                            call: Call<SuccessResponse>,
                                            t: Throwable
                                        ) {
                                        }
                                    }
                                )
                            } else {
                                thread {
                                    db.actionsDao().insertAllActions(ActionEntity(allItems[pageNumber.toInt()].id, false, true, false, false))
                                }
                            }
                            else -> Unit
                        }
                    }

                    override fun onItemLongClick(item: MenuItem?) {
                    }
                }
            )

            rootView!!.source.text = contentSource
            if (typeface != null) {
                rootView!!.source.typeface = typeface
            }

            if (contentText.isEmptyOrNullOrNullString()) {
                getContentFromMercury(customTabsIntent, prefs)
            } else {
                rootView!!.titleView.text = contentTitle
                if (typeface != null) {
                    rootView!!.titleView.typeface = typeface
                }

                htmlToWebview()

                if (!contentImage.isEmptyOrNullOrNullString() && context != null) {
                    rootView!!.imageView.visibility = View.VISIBLE
                    Glide
                        .with(context!!)
                        .asBitmap()
                        .loadMaybeBasicAuth(config, contentImage)
                        .apply(RequestOptions.fitCenterTransform())
                        .into(rootView!!.imageView)
                } else {
                    rootView!!.imageView.visibility = View.GONE
                }
            }

            rootView!!.nestedScrollView.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    if (scrollY > oldScrollY) {
                        fab.hide()
                    } else {
                        if (floatingToolbar.isShowing) floatingToolbar.hide() else fab.show()
                    }
                }
            )

        } catch (e: InflateException) {
            AlertDialog.Builder(context!!)
                .setMessage(context!!.getString(R.string.webview_dialog_issue_message))
                .setTitle(context!!.getString(R.string.webview_dialog_issue_title))
                .setPositiveButton(android.R.string.ok
                ) { dialog, which ->
                    val sharedPref = PreferenceManager.getDefaultSharedPreferences(context!!)
                    val editor = sharedPref.edit()
                    editor.putBoolean("prefer_article_viewer", false)
                    editor.commit()
                    activity!!.finish()
                }
                .create()
                .show()
        }

        return rootView
    }

    private fun refreshAlignment() {
        textAlignment = when (prefs.getInt("text_align", 1)) {
            1 -> "justify"
            2 -> "left"
            else -> "justify"
        }
    }

    private fun getContentFromMercury(
        customTabsIntent: CustomTabsIntent,
        prefs: SharedPreferences
    ) {
        if ((context != null && context!!.isNetworkAccessible(null)) || context == null) {
            rootView!!.progressBar.visibility = View.VISIBLE
            val parser = MercuryApi()

            parser.parseUrl(url).enqueue(
                object : Callback<ParsedContent> {
                    override fun onResponse(
                        call: Call<ParsedContent>,
                        response: Response<ParsedContent>
                    ) {
                        // TODO: clean all the following after finding the mercury content issue
                        try {
                            if (response.body() != null && response.body()!!.content != null && !response.body()!!.content.isNullOrEmpty()) {
                                try {
                                    rootView!!.titleView.text = response.body()!!.title
                                    if (typeface != null) {
                                        rootView!!.titleView.typeface = typeface
                                    }
                                    try {
                                        // Note: Mercury may return relative urls... If it does the url val will not be changed.
                                        URL(response.body()!!.url)
                                        url = response.body()!!.url
                                    } catch (e: MalformedURLException) {
                                        // Mercury returned a relative url. We do nothing.
                                    }
                                } catch (e: Exception) {
                                }

                                try {
                                    contentText = response.body()!!.content.orEmpty()
                                    htmlToWebview()
                                } catch (e: Exception) {
                                }

                                try {
                                    if (response.body()!!.lead_image_url != null && !response.body()!!.lead_image_url.isNullOrEmpty() && context != null) {
                                        rootView!!.imageView.visibility = View.VISIBLE
                                        try {
                                            Glide
                                                .with(context!!)
                                                .asBitmap()
                                                .loadMaybeBasicAuth(config, response.body()!!.lead_image_url.orEmpty())
                                                .apply(RequestOptions.fitCenterTransform())
                                                .into(rootView!!.imageView)
                                        } catch (e: Exception) {
                                        }
                                    } else {
                                        rootView!!.imageView.visibility = View.GONE
                                    }
                                } catch (e: Exception) {
                                    if (context != null) {
                                    }
                                }

                                try {
                                    rootView!!.nestedScrollView.scrollTo(0, 0)

                                    rootView!!.progressBar.visibility = View.GONE
                                } catch (e: Exception) {
                                    if (context != null) {
                                    }
                                }
                            } else {
                                try {
                                    openInBrowserAfterFailing(customTabsIntent)
                                } catch (e: Exception) {
                                    if (context != null) {
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            if (context != null) {
                            }
                        }
                    }

                    override fun onFailure(
                        call: Call<ParsedContent>,
                        t: Throwable
                    ) = openInBrowserAfterFailing(customTabsIntent)
                }
            )
        }
    }

    private fun htmlToWebview() {
        val stringColor = String.format("#%06X", 0xFFFFFF and appColors.colorAccent)

        val attrs: IntArray = intArrayOf(android.R.attr.fontFamily)
        val a: TypedArray = context!!.obtainStyledAttributes(resId, attrs)


        rootView!!.webcontent.settings.standardFontFamily = a.getString(0)
        rootView!!.webcontent.visibility = View.VISIBLE
        val (textColor, backgroundColor) = if (appColors.isDarkTheme) {
            if (context != null) {
                rootView!!.webcontent.setBackgroundColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.dark_webview
                    )
                )
                Pair(ContextCompat.getColor(context!!, R.color.dark_webview_text), ContextCompat.getColor(context!!, R.color.dark_webview))
            } else {
                Pair(null, null)
            }
        } else {
            if (context != null) {
                rootView!!.webcontent.setBackgroundColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.light_webview
                    )
                )
                Pair(ContextCompat.getColor(context!!, R.color.light_webview_text), ContextCompat.getColor(context!!, R.color.light_webview))
            } else {
                Pair(null, null)
            }
        }

        val stringTextColor: String = if (textColor != null) {
            String.format("#%06X", 0xFFFFFF and textColor)
        } else {
            "#000000"
        }

        val stringBackgroundColor = if (backgroundColor != null) {
            String.format("#%06X", 0xFFFFFF and backgroundColor)
        } else {
            "#FFFFFF"
        }

        rootView!!.webcontent.settings.useWideViewPort = true
        rootView!!.webcontent.settings.loadWithOverviewMode = true
        rootView!!.webcontent.settings.javaScriptEnabled = false

        rootView!!.webcontent.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url : String): Boolean {
                if (rootView!!.webcontent.hitTestResult.type != WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    rootView!!.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                return true
            }
        }

        val gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return performClick()
            }
        })

        rootView!!.webcontent.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event)}

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            rootView!!.webcontent.settings.layoutAlgorithm =
                    WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        } else {
            rootView!!.webcontent.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        }

        var baseUrl: String? = null

        try {
            val itemUrl = URL(url)
            baseUrl = itemUrl.protocol + "://" + itemUrl.host
        } catch (e: MalformedURLException) {
        }

        val fontName =  when (font) {
            getString(R.string.open_sans_font_id) -> "Open Sans"
            getString(R.string.roboto_font_id) -> "Roboto"
            else -> ""
        }

        val fontLinkAndStyle = if (font.isNotEmpty()) {
            """<link href="https://fonts.googleapis.com/css?family=${fontName.replace(" ", "+")}" rel="stylesheet">
                |<style>
                |   * {
                |       font-family: '$fontName';
                |   }
                |</style>
            """.trimMargin()
        } else {
            ""
        }

        rootView!!.webcontent.loadDataWithBaseURL(
            baseUrl,
            """<html>
                |<head>
                |   <meta name="viewport" content="width=device-width, initial-scale=1">
                |   <style>
                |      img {
                |        display: inline-block;
                |        height: auto;
                |        width: 100%;
                |        max-width: 100%;
                |      }
                |      a {
                |        color: $stringColor !important;
                |      }
                |      *:not(a) {
                |        color: $stringTextColor;
                |      }
                |      * {
                |        font-size: ${fontSize}px;
                |        text-align: $textAlignment;
                |        word-break: break-word;
                |        overflow:hidden;
                |        line-height: 1.5em;
                |        background-color: $stringBackgroundColor;
                |      }
                |      body, html {
                |        background-color: $stringBackgroundColor !important;
                |        border-color: $stringBackgroundColor  !important;
                |        padding: 0 !important;
                |        margin: 0 !important;
                |      }
                |      a, pre, code {
                |        text-align: $textAlignment;
                |      }
                |      pre, code {
                |        white-space: pre-wrap;
                |        width:100%;
                |        background-color: $stringBackgroundColor;
                |      }
                |   </style>
                |   $fontLinkAndStyle
                |</head>
                |<body>
                |   $contentText
                |</body>""".trimMargin(),
            "text/html",
            "utf-8",
            null
        )
    }

    private fun openInBrowserAfterFailing(customTabsIntent: CustomTabsIntent) {
        rootView!!.progressBar.visibility = View.GONE
        activity!!.openItemUrl(
            allItems,
            pageNumber.toInt(),
            url,
            customTabsIntent,
            true,
            false,
            activity!!
        )
    }

    companion object {
        private const val ARG_POSITION = "position"
        private const val ARG_ITEMS = "items"

        fun newInstance(
            position: Int,
            allItems: ArrayList<Item>
        ): ArticleFragment {
            val fragment = ArticleFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            args.putParcelableArrayList(ARG_ITEMS, allItems)
            fragment.arguments = args
            return fragment
        }
    }

    fun performClick(): Boolean {
        if (rootView!!.webcontent.hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                rootView!!.webcontent.hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {

            val position : Int = allImages.indexOf(rootView!!.webcontent.hitTestResult.extra)


            fragmentManager!!.beginTransaction().replace(R.id.reader_activity_view, ImageFragment.newInstance(position, allImages)).addToBackStack(null).commit()
            return false
        }
        return false
    }


}
