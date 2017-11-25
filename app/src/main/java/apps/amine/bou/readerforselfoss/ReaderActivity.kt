package apps.amine.bou.readerforselfoss

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.NestedScrollView
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import apps.amine.bou.readerforselfoss.api.mercury.MercuryApi
import apps.amine.bou.readerforselfoss.api.mercury.ParsedContent
import apps.amine.bou.readerforselfoss.utils.buildCustomTabsIntent
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.isEmptyOrNullOrNullString
import apps.amine.bou.readerforselfoss.utils.openItemUrl
import apps.amine.bou.readerforselfoss.utils.shareLink
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.crashlytics.android.Crashlytics
import com.ftinc.scoop.Scoop
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import kotlinx.android.synthetic.main.activity_reader.*
import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReaderActivity : AppCompatActivity() {
    private lateinit var mCustomTabActivityHelper: CustomTabActivityHelper
    //private lateinit var content: HtmlTextView
    private lateinit var url: String
    private lateinit var contentText: String
    private lateinit var contentSource: String
    private lateinit var contentImage: String
    private lateinit var contentTitle: String
    private lateinit var fab: FloatingActionButton

    override fun onStop() {
        super.onStop()
        mCustomTabActivityHelper.unbindCustomTabsService(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scoop.getInstance().apply(this)
        setContentView(R.layout.activity_reader)

        url = intent.getStringExtra("url")
        contentText = intent.getStringExtra("content")
        contentTitle = intent.getStringExtra("title")
        contentImage = intent.getStringExtra("image")
        contentSource = intent.getStringExtra("source")

        fab = findViewById(R.id.fab)
        val mFloatingToolbar: FloatingToolbar = findViewById(R.id.floatingToolbar)
        mFloatingToolbar.attachFab(fab)

        val customTabsIntent = this@ReaderActivity.buildCustomTabsIntent()
        mCustomTabActivityHelper = CustomTabActivityHelper()
        mCustomTabActivityHelper.bindCustomTabsService(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        mFloatingToolbar.setClickListener(object : FloatingToolbar.ItemClickListener {
            override fun onItemClick(item: MenuItem) {
                when (item.itemId) {
                    R.id.more_action -> getContentFromMercury(customTabsIntent, prefs)
                    R.id.share_action -> this@ReaderActivity.shareLink(url)
                    R.id.open_action -> this@ReaderActivity.openItemUrl(
                            url,
                            contentText,
                            contentImage,
                            contentTitle,
                            contentSource,
                            customTabsIntent,
                            false,
                            false,
                            this@ReaderActivity
                    )
                    else -> Unit
                }
            }

            override fun onItemLongClick(item: MenuItem?) {
            }
        })


        if (contentText.isEmptyOrNullOrNullString()) {
            getContentFromMercury(customTabsIntent, prefs)
        } else {
            source.text = contentSource
            titleView.text = contentTitle
            tryToHandleHtml(contentText, customTabsIntent, prefs)

            if (!contentImage.isEmptyOrNullOrNullString()) {
                imageView.visibility = View.VISIBLE
                Glide
                        .with(baseContext)
                        .asBitmap()
                        .load(contentImage)
                        .apply(RequestOptions.fitCenterTransform())
                        .into(imageView)
            } else {
                imageView.visibility = View.GONE
            }
        }

        nestedScrollView.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    if (scrollY > oldScrollY) {
                        fab.hide()
                    } else {
                        if (mFloatingToolbar.isShowing) mFloatingToolbar.hide() else fab.show()
                    }
                }
        )

        content.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun getContentFromMercury(
            customTabsIntent: CustomTabsIntent,
            prefs: SharedPreferences
    ) {
        progressBar.visibility = View.VISIBLE
        val parser = MercuryApi(
                BuildConfig.MERCURY_KEY,
                prefs.getBoolean("should_log_everything", false)
        )

        parser.parseUrl(url).enqueue(object : Callback<ParsedContent> {
            override fun onResponse(
                    call: Call<ParsedContent>,
                    response: Response<ParsedContent>
            ) {
                if (response.body() != null && response.body()!!.content != null && response.body()!!.content.isNotEmpty()) {
                    source.text = response.body()!!.domain
                    titleView.text = response.body()!!.title
                    this@ReaderActivity.url = response.body()!!.url

                    if (response.body()!!.content != null && !response.body()!!.content.isEmpty()) {
                        tryToHandleHtml(response.body()!!.content, customTabsIntent, prefs)
                    }

                    if (response.body()!!.lead_image_url != null && !response.body()!!.lead_image_url.isEmpty()) {
                        imageView.visibility = View.VISIBLE
                        Glide
                                .with(baseContext)
                                .asBitmap()
                                .load(response.body()!!.lead_image_url)
                                .apply(RequestOptions.fitCenterTransform())
                                .into(imageView)
                    } else {
                        imageView.visibility = View.GONE
                    }

                    nestedScrollView.scrollTo(0, 0)

                    progressBar.visibility = View.GONE
                } else {
                    openInBrowserAfterFailing(customTabsIntent)
                }
            }

            override fun onFailure(
                    call: Call<ParsedContent>,
                    t: Throwable
            ) = openInBrowserAfterFailing(customTabsIntent)
        })
    }

    private fun tryToHandleHtml(
            c: String,
            customTabsIntent: CustomTabsIntent,
            prefs: SharedPreferences
    ) {
        try {
            content.text = Html.fromHtml(c, HtmlHttpImageGetter(content, null, true), null)

            //content.setHtml(response.body()!!.content, HtmlHttpImageGetter(content, null, true))
        } catch (e: Exception) {
            Crashlytics.setUserIdentifier(prefs.getString("unique_id", ""))
            Crashlytics.log(100, "CANT_TRANSFORM_TO_HTML", e.message)
            Crashlytics.logException(e)
            openInBrowserAfterFailing(customTabsIntent)
        }
    }

    private fun openInBrowserAfterFailing(customTabsIntent: CustomTabsIntent) {
        progressBar.visibility = View.GONE
        this@ReaderActivity.openItemUrl(
                url,
                contentText,
                contentImage,
                contentTitle,
                contentSource,
                customTabsIntent,
                true,
                false,
                this@ReaderActivity
        )
        finish()
    }
}
