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
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.widget.Toast
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
import apps.amine.bou.readerforselfoss.utils.toPx
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.crashlytics.android.Crashlytics
import com.ftinc.scoop.Scoop
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import kotlinx.android.synthetic.main.fragment_article.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.MalformedURLException
import java.net.URL

class ArticleFragment : Fragment() {
    private lateinit var pageNumber: Number
    private var fontSize: Int = 14
    private lateinit var allItems: ArrayList<Item>
    private lateinit var mCustomTabActivityHelper: CustomTabActivityHelper
    private lateinit var url: String
    private lateinit var contentText: String
    private lateinit var contentSource: String
    private lateinit var contentImage: String
    private lateinit var contentTitle: String
    private lateinit var fab: FloatingActionButton

    override fun onStop() {
        super.onStop()
        mCustomTabActivityHelper.unbindCustomTabsService(activity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageNumber = arguments!!.getInt(ARG_POSITION)
        allItems = arguments!!.getParcelableArrayList(ARG_ITEMS)
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
        fontSize = prefs.getString("reader_font_size", "14").toInt()


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

            htmlToWebview(contentText, prefs)

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
                    // TODO: clean all the following after finding the mercury content issue
                    try {
                        if (response.body() != null && response.body()!!.content != null && !response.body()!!.content.isNullOrEmpty()) {
                            try {
                                rootView.source.text = response.body()!!.domain
                                rootView.titleView.text = response.body()!!.title
                                url = response.body()!!.url
                            } catch (e: Exception) {
                                Crashlytics.setUserIdentifier(prefs.getString("unique_id", ""))
                                Crashlytics.log(
                                    100,
                                    "MERCURY_CONTENT_EXCEPTION",
                                    "source titleView or url issues"
                                )
                                Crashlytics.logException(e)
                            }

                            try {
                                htmlToWebview(response.body()!!.content.orEmpty(), prefs)
                            } catch (e: Exception) {
                                Crashlytics.setUserIdentifier(prefs.getString("unique_id", ""))
                                Crashlytics.log(
                                    100,
                                    "MERCURY_CONTENT_EXCEPTION",
                                    "Webview issue"
                                )
                                Crashlytics.logException(e)
                            }

                            try {
                                if (response.body()!!.lead_image_url != null && !response.body()!!.lead_image_url.isNullOrEmpty()) {
                                    rootView.imageView.visibility = View.VISIBLE
                                    try {
                                        Glide
                                            .with(activity!!.baseContext)
                                            .asBitmap()
                                            .load(response.body()!!.lead_image_url)
                                            .apply(RequestOptions.fitCenterTransform())
                                            .into(rootView.imageView)
                                    } catch (e: Exception) {
                                        Crashlytics.setUserIdentifier(
                                            prefs.getString(
                                                "unique_id",
                                                ""
                                            )
                                        )
                                        Crashlytics.log(
                                            100,
                                            "MERCURY_CONTENT_EXCEPTION",
                                            "Glide issue"
                                        )
                                        Crashlytics.logException(e)
                                    }
                                } else {
                                    rootView.imageView.visibility = View.GONE
                                }
                            } catch (e: Exception) {
                                Crashlytics.setUserIdentifier(prefs.getString("unique_id", ""))
                                Crashlytics.log(
                                    100,
                                    "MERCURY_CONTENT_EXCEPTION",
                                    "Glide or image issue"
                                )
                                Crashlytics.logException(e)
                            }

                            try {
                                rootView.nestedScrollView.scrollTo(0, 0)

                                rootView.progressBar.visibility = View.GONE
                            } catch (e: Exception) {
                                Crashlytics.setUserIdentifier(prefs.getString("unique_id", ""))
                                Crashlytics.log(
                                    100,
                                    "MERCURY_CONTENT_EXCEPTION",
                                    "Scroll or visibility issues"
                                )
                                Crashlytics.logException(e)
                            }
                        } else {
                            try {
                                openInBrowserAfterFailing(customTabsIntent)
                            } catch (e: Exception) {
                                Crashlytics.setUserIdentifier(prefs.getString("unique_id", ""))
                                Crashlytics.log(
                                    100,
                                    "MERCURY_CONTENT_EXCEPTION",
                                    "Browser after failing issue"
                                )
                                Crashlytics.logException(e)
                            }
                        }
                    } catch (e: Exception) {
                        Crashlytics.setUserIdentifier(prefs.getString("unique_id", ""))
                        Crashlytics.log(
                            100,
                            "MERCURY_CONTENT_EXCEPTION",
                            "UNCAUGHT (?) Fatal Exception on mercury response"
                        )
                        Crashlytics.logException(e)
                    }
                }

                override fun onFailure(
                    call: Call<ParsedContent>,
                    t: Throwable
                ) = openInBrowserAfterFailing(customTabsIntent)
            }
        )
    }

    private fun htmlToWebview(c: String, prefs: SharedPreferences) {

        val accentColor = ContextCompat.getColor(activity!!.baseContext, R.color.accent)
        val stringColor = String.format("#%06X", 0xFFFFFF and accentColor)

        rootView.webcontent.visibility = View.VISIBLE
        val textColor = if (Scoop.getInstance().currentFlavor.isDayNight) {
            rootView.webcontent.setBackgroundColor(
                ContextCompat.getColor(
                    activity!!.baseContext,
                    R.color.dark_webview
                )
            )
            ContextCompat.getColor(activity!!.baseContext, R.color.dark_webview_text)
        } else {
            rootView.webcontent.setBackgroundColor(
                ContextCompat.getColor(
                    activity!!.baseContext,
                    R.color.light_webview
                )
            )
            ContextCompat.getColor(activity!!.baseContext, R.color.light_webview_text)
        }

        val stringTextColor = String.format("#%06X", 0xFFFFFF and textColor)

        rootView.webcontent.settings.useWideViewPort = true
        rootView.webcontent.settings.loadWithOverviewMode = true
        rootView.webcontent.settings.javaScriptEnabled = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            rootView.webcontent.settings.layoutAlgorithm =
                    WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        } else {
            rootView.webcontent.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        }

        var baseUrl: String? = null

        try {
            val itemUrl = URL(url)
            baseUrl = itemUrl.protocol + "://" + itemUrl.host
        } catch (e: MalformedURLException) {
            Toast.makeText(activity!!.baseContext, "You are encountering a bug that I can't solve. Can you please contact me to solve the issue, please ?", Toast.LENGTH_LONG).show()
            Crashlytics.setUserIdentifier(prefs.getString("unique_id", ""))
            Crashlytics.log(100, "BASE_URL_MALFORMED", e.message)
            Crashlytics.logException(e)
        }

        rootView.webcontent.loadDataWithBaseURL(
            baseUrl,
            """<style>
                |img {
                |  display: inline-block;
                |  height: auto;
                |  width: 100%;
                |  max-width: 100%;
                |}
                |a {
                |  color: $stringColor !important;
                |}
                |*:not(a) {
                |  color: $stringTextColor;
                |}
                |* {
                |  font-size: ${fontSize.toPx}px;
                |  text-align: justify;
                |  word-break: break-word;
                |  overflow:hidden;
                |}
                |a, pre, code {
                |  text-align: left;
                |}
                |pre, code {
                |  white-space: pre-wrap;
                |  width:100%;
                |  background-color: #EEEEEE;
                |}</style>$c""".trimMargin(),
            "text/html; charset=utf-8",
            "utf-8",
            null
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
}
