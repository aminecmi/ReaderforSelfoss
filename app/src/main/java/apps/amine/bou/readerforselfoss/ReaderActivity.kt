package apps.amine.bou.readerforselfoss

import android.os.Bundle
import android.os.PersistableBundle
import android.preference.PreferenceManager
import android.support.customtabs.CustomTabsIntent
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import apps.amine.bou.readerforselfoss.R.id.fab
import apps.amine.bou.readerforselfoss.api.mercury.MercuryApi
import apps.amine.bou.readerforselfoss.api.mercury.ParsedContent
import apps.amine.bou.readerforselfoss.utils.buildCustomTabsIntent
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.isEmptyOrNullOrNullString
import apps.amine.bou.readerforselfoss.utils.openItemUrl
import apps.amine.bou.readerforselfoss.utils.shareLink
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ftinc.scoop.Scoop
import com.github.rubensousa.floatingtoolbar.FloatingToolbar
import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter
import org.sufficientlysecure.htmltextview.HtmlTextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import xyz.klinker.android.drag_dismiss.activity.DragDismissActivity


class ReaderActivity : AppCompatActivity() {
    private lateinit var mCustomTabActivityHelper: CustomTabActivityHelper
    private lateinit var image: ImageView
    private lateinit var source: TextView
    private lateinit var title: TextView
    private lateinit var content: TextView
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
        val v = this

        setContentView(R.layout.activity_reader)


        image = v.findViewById(R.id.imageView)
        source = v.findViewById(R.id.source)
        title = v.findViewById(R.id.title)
        content = v.findViewById(R.id.content)
        url = intent.getStringExtra("url")
        contentText = intent.getStringExtra("content")
        contentTitle = intent.getStringExtra("title")
        contentImage = intent.getStringExtra("image")
        contentSource = intent.getStringExtra("source")

        fab = v.findViewById(R.id.fab)
        val mFloatingToolbar: FloatingToolbar = v.findViewById(R.id.floatingToolbar)
        mFloatingToolbar.attachFab(fab)

        val customTabsIntent = this@ReaderActivity.buildCustomTabsIntent()
        mCustomTabActivityHelper = CustomTabActivityHelper()
        mCustomTabActivityHelper.bindCustomTabsService(this)


        mFloatingToolbar.setClickListener(object : FloatingToolbar.ItemClickListener {
            override fun onItemClick(item: MenuItem) {
                when (item.itemId) {
                    R.id.more_action -> getContentFromMercury(customTabsIntent)
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
                            this@ReaderActivity)
                    else -> Unit
                }
            }

            override fun onItemLongClick(item: MenuItem?) {
            }
        })


        if (contentText.isEmptyOrNullOrNullString()) {
            getContentFromMercury(customTabsIntent)
        } else {
            source.text = contentSource
            title.text = contentTitle
            content.text = Html.fromHtml(contentText, HtmlHttpImageGetter(content, null, true), null)
            //content.setHtml(contentText, HtmlHttpImageGetter(content, null, true))

            if (!contentImage.isEmptyOrNullOrNullString())
                Glide
                        .with(baseContext)
                        .asBitmap()
                        .load(contentImage)
                        .apply(RequestOptions.fitCenterTransform())
                        .into(image)
        }
    }

    private fun getContentFromMercury(customTabsIntent: CustomTabsIntent) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val parser = MercuryApi(BuildConfig.MERCURY_KEY, prefs.getBoolean("should_log_everything", false))

        parser.parseUrl(url).enqueue(object : Callback<ParsedContent> {
            override fun onResponse(call: Call<ParsedContent>, response: Response<ParsedContent>) {
                if (response.body() != null && response.body()!!.content != null && response.body()!!.content.isNotEmpty()) {
                    source.text = response.body()!!.domain
                    title.text = response.body()!!.title
                    this@ReaderActivity.url = response.body()!!.url
                    if (response.body()!!.content != null && !response.body()!!.content.isEmpty()) {
                        try {
                            content.text = Html.fromHtml(response.body()!!.content, HtmlHttpImageGetter(content, null, true), null)

                            //content.setHtml(response.body()!!.content, HtmlHttpImageGetter(content, null, true))
                        } catch (e: IndexOutOfBoundsException) {
                            openInBrowserAfterFailing()
                        }
                    }
                    if (response.body()!!.lead_image_url != null && !response.body()!!.lead_image_url.isEmpty())
                        Glide
                                .with(baseContext)
                                .asBitmap()
                                .load(response.body()!!.lead_image_url)
                                .apply(RequestOptions.fitCenterTransform())
                                .into(image)

                } else openInBrowserAfterFailing()
            }

            override fun onFailure(call: Call<ParsedContent>, t: Throwable) = openInBrowserAfterFailing()

            private fun openInBrowserAfterFailing() {
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
        })
    }
}
