package apps.amine.bou.readerforselfoss.fragments

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.NestedScrollView
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import apps.amine.bou.readerforselfoss.BuildConfig
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.mercury.MercuryApi
import apps.amine.bou.readerforselfoss.api.mercury.ParsedContent
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.utils.buildCustomTabsIntent
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.isEmptyOrNullOrNullString
import apps.amine.bou.readerforselfoss.utils.openItemUrl
import apps.amine.bou.readerforselfoss.utils.shareLink
import apps.amine.bou.readerforselfoss.utils.sourceAndDateText
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.crashlytics.android.Crashlytics
import com.ftinc.scoop.Scoop
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import kotlinx.android.synthetic.main.fragment_article.view.*
import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ArticleFragment : Fragment() {
    private lateinit var pageNumber: Number
    private lateinit var allItems: ArrayList<Item>
    private lateinit var mCustomTabActivityHelper: CustomTabActivityHelper
    private lateinit var url: String
    private lateinit var contentText: String
    private lateinit var contentSource: String
    private lateinit var contentImage: String
    private lateinit var contentTitle: String
    private lateinit var fab: FloatingActionButton

    private var useWebview: Boolean = false

    override fun onStop() {
        super.onStop()
        mCustomTabActivityHelper.unbindCustomTabsService(activity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageNumber = arguments!!.getInt(ARG_POSITION)
        allItems = arguments!!.getParcelableArrayList(ARG_ITEMS)
        useWebview = arguments!!.getBoolean(ARG_WEBVIEW)
    }

    private lateinit var rootView: ViewGroup

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        rootView = inflater
                .inflate(R.layout.fragment_article, container, false) as ViewGroup

        url = allItems[pageNumber.toInt()].getLinkDecoded()
        contentText = allItems[pageNumber.toInt()].content
        contentTitle = allItems[pageNumber.toInt()].title
        contentImage = allItems[pageNumber.toInt()].getThumbnail(activity!!)
        contentSource = allItems[pageNumber.toInt()].sourceAndDateText()

        fab = rootView.fab
        val mFloatingToolbar: FloatingToolbar = rootView.floatingToolbar
        mFloatingToolbar.attachFab(fab)

        val customTabsIntent = activity!!.buildCustomTabsIntent()
        mCustomTabActivityHelper = CustomTabActivityHelper()
        mCustomTabActivityHelper.bindCustomTabsService(activity)

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)

        mFloatingToolbar.setClickListener(
                object : FloatingToolbar.ItemClickListener {
                    override fun onItemClick(item: MenuItem) {
                        when (item.itemId) {
                            R.id.more_action -> getContentFromMercury(customTabsIntent, prefs)
                            R.id.share_action -> activity!!.shareLink(url)
                            R.id.open_action -> activity!!.openItemUrl(
                                    allItems,
                                    pageNumber.toInt(),
                                    url,
                                    customTabsIntent,
                                    false,
                                    false,
                                    activity!!
                            )
                            else -> Unit
                        }
                    }

                    override fun onItemLongClick(item: MenuItem?) {
                    }
                }
        )


        if (contentText.isEmptyOrNullOrNullString()) {
            getContentFromMercury(customTabsIntent, prefs)
        } else {
            rootView.source.text = contentSource
            rootView.titleView.text = contentTitle

            if (!useWebview) {
                htmlToTextview(contentText, customTabsIntent, prefs)
            } else {
                htmlToWebview(contentText)
            }

            if (!contentImage.isEmptyOrNullOrNullString()) {
                rootView.imageView.visibility = View.VISIBLE
                Glide
                        .with(activity!!.baseContext)
                        .asBitmap()
                        .load(contentImage)
                        .apply(RequestOptions.fitCenterTransform())
                        .into(rootView.imageView)
            } else {
                rootView.imageView.visibility = View.GONE
            }
        }

        rootView.nestedScrollView.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    if (scrollY > oldScrollY) {
                        fab.hide()
                    } else {
                        if (mFloatingToolbar.isShowing) mFloatingToolbar.hide() else fab.show()
                    }
                }
        )

        if (!useWebview) {
            rootView.content.movementMethod = LinkMovementMethod.getInstance()
        }

        return rootView
    }

    private fun getContentFromMercury(
            customTabsIntent: CustomTabsIntent,
            prefs: SharedPreferences
    ) {
        rootView.progressBar.visibility = View.VISIBLE
        val parser = MercuryApi(
                BuildConfig.MERCURY_KEY,
                prefs.getBoolean("should_log_everything", false)
        )

        parser.parseUrl(url).enqueue(
                object : Callback<ParsedContent> {
                    override fun onResponse(
                            call: Call<ParsedContent>,
                            response: Response<ParsedContent>
                    ) {
                        if (response.body() != null && response.body()!!.content != null && response.body()!!.content.isNotEmpty()) {
                            rootView.source.text = response.body()!!.domain
                            rootView.titleView.text = response.body()!!.title
                            url = response.body()!!.url

                            if (response.body()!!.content != null && !response.body()!!.content.isEmpty()) {
                                if (!useWebview) {
                                    htmlToTextview(
                                            response.body()!!.content,
                                            customTabsIntent,
                                            prefs
                                    )
                                } else {
                                    htmlToWebview(response.body()!!.content)
                                }
                            }

                            if (response.body()!!.lead_image_url != null && !response.body()!!.lead_image_url.isEmpty()) {
                                rootView.imageView.visibility = View.VISIBLE
                                Glide
                                        .with(activity!!.baseContext)
                                        .asBitmap()
                                        .load(response.body()!!.lead_image_url)
                                        .apply(RequestOptions.fitCenterTransform())
                                        .into(rootView.imageView)
                            } else {
                                rootView.imageView.visibility = View.GONE
                            }

                            rootView.nestedScrollView.scrollTo(0, 0)

                            rootView.progressBar.visibility = View.GONE
                        } else {
                            openInBrowserAfterFailing(customTabsIntent)
                        }
                    }

                    override fun onFailure(
                            call: Call<ParsedContent>,
                            t: Throwable
                    ) = openInBrowserAfterFailing(customTabsIntent)
                }
        )
    }

    private fun htmlToTextview(
            c: String,
            customTabsIntent: CustomTabsIntent,
            prefs: SharedPreferences
    ) {
        try {
            rootView.content.visibility = View.VISIBLE
            rootView.content.text = Html.fromHtml(
                    c,
                    HtmlHttpImageGetter(rootView.content, null, true),
                    null
            )
        } catch (e: Exception) {
            Crashlytics.setUserIdentifier(prefs.getString("unique_id", ""))
            Crashlytics.log(100, "CANT_TRANSFORM_TO_HTML", e.message)
            Crashlytics.logException(e)
            openInBrowserAfterFailing(customTabsIntent)
        }
    }

    private fun htmlToWebview(c: String) {

        val defaultColor = TypedValue()
        activity!!.baseContext.theme.resolveAttribute(
                android.R.attr.colorAccent,
                defaultColor,
                true
        )
        val accentColor = fab.backgroundTintList?.defaultColor ?: defaultColor.data
        val stringColor = String.format("#%06X", 0xFFFFFF and accentColor)

        rootView.webcontent.visibility = View.VISIBLE
        val textColor = if (Scoop.getInstance().currentFlavor.isDayNight) {
            rootView.webcontent.setBackgroundColor(ContextCompat.getColor(activity!!.baseContext, R.color.dark_webview))
            ContextCompat.getColor(activity!!.baseContext, R.color.dark_webview_text)
        } else {
            rootView.webcontent.setBackgroundColor(ContextCompat.getColor(activity!!.baseContext, R.color.light_webview))
            ContextCompat.getColor(activity!!.baseContext, R.color.light_webview_text)
        }

        val stringTextColor = String.format("#%06X", 0xFFFFFF and textColor)

        rootView.webcontent.settings.useWideViewPort = true
        rootView.webcontent.settings.loadWithOverviewMode = true
        rootView.webcontent.settings.javaScriptEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            rootView.webcontent.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        } else {
            rootView.webcontent.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        }

        rootView.webcontent.loadData(
                "<style>img{display: inline-block;height: auto; width: 100%; max-width: 100%;} a{color: $stringColor;} *:not(a){color: $stringTextColor;}</style>$c",
                "text/html; charset=utf-8",
                "utf-8"
        )
    }

    private fun openInBrowserAfterFailing(customTabsIntent: CustomTabsIntent) {
        rootView.progressBar.visibility = View.GONE
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
        private val ARG_POSITION = "position"
        private val ARG_ITEMS = "items"
        private val ARG_WEBVIEW = "userWebview"

        fun newInstance(
                position: Int,
                allItems: ArrayList<Item>,
                webview: Boolean
        ): ArticleFragment {
            val fragment = ArticleFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            args.putBoolean(ARG_WEBVIEW, webview)
            args.putParcelableArrayList(ARG_ITEMS, allItems)
            fragment.arguments = args
            return fragment
        }
    }
}
