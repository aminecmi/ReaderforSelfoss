package apps.amine.bou.readerforselfoss.fragments

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.browser.customtabs.CustomTabsIntent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.mercury.MercuryApi
import apps.amine.bou.readerforselfoss.api.mercury.ParsedContent
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.buildCustomTabsIntent
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.isEmptyOrNullOrNullString
import apps.amine.bou.readerforselfoss.utils.maybeHandleSilentException
import apps.amine.bou.readerforselfoss.utils.openItemUrl
import apps.amine.bou.readerforselfoss.utils.shareLink
import apps.amine.bou.readerforselfoss.utils.sourceAndDateText
import apps.amine.bou.readerforselfoss.utils.succeeded
import apps.amine.bou.readerforselfoss.utils.toPx
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import kotlinx.android.synthetic.main.fragment_article.view.*
import org.acra.ACRA
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
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var fab: FloatingActionButton
    private lateinit var appColors: AppColors

    override fun onStop() {
        super.onStop()
        mCustomTabActivityHelper.unbindCustomTabsService(activity)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appColors = AppColors(activity!!)

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

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        editor = prefs.edit()
        fontSize = prefs.getString("reader_font_size", "14").toInt()

        val settings = activity!!.getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
        val debugReadingItems = prefs.getBoolean("read_debug", false)

        val api = SelfossApi(
            context!!,
            activity!!,
            settings.getBoolean("isSelfSignedCert", false),
            prefs.getBoolean("should_log_everything", false)
        )

        fab = rootView.fab

        fab.backgroundTintList = ColorStateList.valueOf(appColors.colorAccent)

        fab.rippleColor = appColors.colorAccentDark

        val floatingToolbar: FloatingToolbar = rootView.floatingToolbar
        floatingToolbar.attachFab(fab)

        floatingToolbar.background = ColorDrawable(appColors.colorAccent)

        val customTabsIntent = activity!!.buildCustomTabsIntent()
        mCustomTabActivityHelper = CustomTabActivityHelper()
        mCustomTabActivityHelper.bindCustomTabsService(activity)


        floatingToolbar.setClickListener(
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
                        R.id.unread_action -> api.unmarkItem(allItems[pageNumber.toInt()].id).enqueue(
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
                                        ACRA.getErrorReporter().maybeHandleSilentException(Exception(message), activity!!)
                                    }
                                }

                                override fun onFailure(
                                    call: Call<SuccessResponse>,
                                    t: Throwable
                                ) {
                                    if (debugReadingItems) {
                                        ACRA.getErrorReporter().maybeHandleSilentException(t, activity!!)
                                    }
                                }
                            }
                        )
                        else -> Unit
                    }
                }

                override fun onItemLongClick(item: MenuItem?) {
                }
            }
        )

        rootView.source.text = contentSource

        if (contentText.isEmptyOrNullOrNullString()) {
            getContentFromMercury(customTabsIntent, prefs)
        } else {
            rootView.titleView.text = contentTitle

            htmlToWebview(contentText, prefs)

            if (!contentImage.isEmptyOrNullOrNullString() && context != null) {
                rootView.imageView.visibility = View.VISIBLE
                Glide
                    .with(context!!)
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
                    if (floatingToolbar.isShowing) floatingToolbar.hide() else fab.show()
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
                                rootView.titleView.text = response.body()!!.title
                                try {
                                    // Note: Mercury may return relative urls... If it does the url val will not be changed.
                                    URL(response.body()!!.url)
                                    url = response.body()!!.url
                                } catch (e: MalformedURLException) {
                                    ACRA.getErrorReporter().maybeHandleSilentException(e, activity!!)
                                }
                            } catch (e: Exception) {
                                if (context != null) {
                                    ACRA.getErrorReporter().maybeHandleSilentException(e, context!!)
                                }
                            }

                            try {
                                htmlToWebview(response.body()!!.content.orEmpty(), prefs)
                            } catch (e: Exception) {
                                if (context != null) {
                                    ACRA.getErrorReporter().maybeHandleSilentException(e, context!!)
                                }
                            }

                            try {
                                if (response.body()!!.lead_image_url != null && !response.body()!!.lead_image_url.isNullOrEmpty() && context != null) {
                                    rootView.imageView.visibility = View.VISIBLE
                                    try {
                                        Glide
                                            .with(context!!)
                                            .asBitmap()
                                            .load(response.body()!!.lead_image_url)
                                            .apply(RequestOptions.fitCenterTransform())
                                            .into(rootView.imageView)
                                    } catch (e: Exception) {
                                        ACRA.getErrorReporter().maybeHandleSilentException(e, context!!)
                                    }
                                } else {
                                    rootView.imageView.visibility = View.GONE
                                }
                            } catch (e: Exception) {
                                if (context != null) {
                                    ACRA.getErrorReporter().maybeHandleSilentException(e, context!!)
                                }
                            }

                            try {
                                rootView.nestedScrollView.scrollTo(0, 0)

                                rootView.progressBar.visibility = View.GONE
                            } catch (e: Exception) {
                                if (context != null) {
                                    ACRA.getErrorReporter().maybeHandleSilentException(e, context!!)
                                }
                            }
                        } else {
                            try {
                                openInBrowserAfterFailing(customTabsIntent)
                            } catch (e: Exception) {
                                if (context != null) {
                                    ACRA.getErrorReporter().maybeHandleSilentException(e, context!!)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (context != null) {
                            ACRA.getErrorReporter().maybeHandleSilentException(e, context!!)
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

    private fun htmlToWebview(c: String, prefs: SharedPreferences) {
        val stringColor = String.format("#%06X", 0xFFFFFF and appColors.colorAccent)

        rootView.webcontent.visibility = View.VISIBLE
        val (textColor, backgroundColor) = if (appColors.isDarkTheme) {
            if (context != null) {
                rootView.webcontent.setBackgroundColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.dark_webview
                    )
                )
                Pair(ContextCompat.getColor(context!!, R.color.dark_webview_text), ContextCompat.getColor(context!!, R.color.light_webview_text))
            } else {
                Pair(null, null)
            }
        } else {
            if (context != null) {
                rootView.webcontent.setBackgroundColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.light_webview
                    )
                )
                Pair(ContextCompat.getColor(context!!, R.color.light_webview_text), ContextCompat.getColor(context!!, R.color.dark_webview_text))
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
            ACRA.getErrorReporter().maybeHandleSilentException(e, activity!!)
        }

        rootView.webcontent.loadDataWithBaseURL(
            baseUrl,
            """<html>
                |<head>
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
                |        font-size: ${fontSize.toPx}px;
                |        text-align: justify;
                |        word-break: break-word;
                |        overflow:hidden;
                |      }
                |      a, pre, code {
                |        text-align: left;
                |      }
                |      pre, code {
                |        white-space: pre-wrap;
                |        width:100%;
                |        background-color: $stringBackgroundColor;
                |      }
                |   </style>
                |</head>
                |<body>
                |   $c
                |</body>""".trimMargin(),
            "text/html",
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


}
