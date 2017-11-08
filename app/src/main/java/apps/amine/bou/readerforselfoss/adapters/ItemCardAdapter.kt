package apps.amine.bou.readerforselfoss.adapters

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.support.design.widget.Snackbar
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.TextView
import android.widget.Toast
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.utils.buildCustomTabsIntent
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.glide.bitmapCenterCrop
import apps.amine.bou.readerforselfoss.utils.glide.circularBitmapDrawable
import apps.amine.bou.readerforselfoss.utils.openInBrowserAsNewTask
import apps.amine.bou.readerforselfoss.utils.openItemUrl
import apps.amine.bou.readerforselfoss.utils.shareLink
import apps.amine.bou.readerforselfoss.utils.sourceAndDateText
import apps.amine.bou.readerforselfoss.utils.succeeded
import apps.amine.bou.readerforselfoss.utils.toTextDrawableString
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.like.LikeButton
import com.like.OnLikeListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ItemCardAdapter(private val app: Activity,
                      private val items: ArrayList<Item>,
                      private val api: SelfossApi,
                      private val helper: CustomTabActivityHelper,
                      private val internalBrowser: Boolean,
                      private val articleViewer: Boolean,
                      private val fullHeightCards: Boolean,
                      private val appColors: AppColors,
                      val debugReadingItems: Boolean,
                      val userIdentifier: String) : RecyclerView.Adapter<ItemCardAdapter.ViewHolder>() {
    private val c: Context = app.baseContext
    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.card_item, parent, false) as CardView
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itm = items[position]


        holder.saveBtn.isLiked = itm.starred
        holder.title.text = Html.fromHtml(itm.title)

        holder.sourceTitleAndDate.text = itm.sourceAndDateText()

        if (itm.getThumbnail(c).isEmpty()) {
            Glide.with(c).clear(holder.itemImage)
            holder.itemImage.setImageDrawable(null)
        } else {
            c.bitmapCenterCrop(itm.getThumbnail(c), holder.itemImage)
        }

        if (itm.getIcon(c).isEmpty()) {
            val color = generator.getColor(itm.sourcetitle)

            val drawable =
                TextDrawable
                    .builder()
                    .round()
                    .build(itm.sourcetitle.toTextDrawableString(), color)
            holder.sourceImage.setImageDrawable(drawable)
        } else {
            c.circularBitmapDrawable(itm.getIcon(c), holder.sourceImage)
        }

        holder.saveBtn.isLiked = itm.starred
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun doUnmark(i: Item, position: Int) {
        val s = Snackbar
                .make(app.findViewById(R.id.coordLayout), R.string.marked_as_read, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo_string) {
                    items.add(position, i)
                    notifyItemInserted(position)

                    api.unmarkItem(i.id).enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {}

                        override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                            items.remove(i)
                            notifyItemRemoved(position)
                            doUnmark(i, position)
                        }
                    })
                }

        val view = s.view
        val tv: TextView = view.findViewById(android.support.design.R.id.snackbar_text)
        tv.setTextColor(Color.WHITE)
        s.show()
    }

    fun removeItemAtIndex(position: Int) {

        val i = items[position]

        items.remove(i)
        notifyItemRemoved(position)

        api.markItem(i.id).enqueue(object : Callback<SuccessResponse> {
            override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {
                if (!response.succeeded() && debugReadingItems) {
                    val message =
                        "message: ${response.message()} " +
                            "response isSuccess: ${response.isSuccessful} " +
                            "response code: ${response.code()} " +
                            "response message: ${response.message()} " +
                            "response errorBody: ${response.errorBody()?.string()} " +
                            "body success: ${response.body()?.success} " +
                            "body isSuccess: ${response.body()?.isSuccess}"
                    Crashlytics.setUserIdentifier(userIdentifier)
                    Crashlytics.log(100, "READ_DEBUG_SUCCESS", message)
                    Crashlytics.logException(Exception("Was success, but did it work ?"))

                    Toast.makeText(c, message, Toast.LENGTH_LONG).show()
                }
                doUnmark(i, position)
            }

            override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                if (debugReadingItems) {
                    Crashlytics.setUserIdentifier(userIdentifier)
                    Crashlytics.log(100, "READ_DEBUG_ERROR", t.message)
                    Crashlytics.logException(t)
                    Toast.makeText(c, t.message, Toast.LENGTH_LONG).show()
                }
                Toast.makeText(app, app.getString(R.string.cant_mark_read), Toast.LENGTH_SHORT).show()
                items.add(i)
                notifyItemInserted(position)
            }
        })

    }

    inner class ViewHolder(val mView: CardView) : RecyclerView.ViewHolder(mView) {
        lateinit var saveBtn: LikeButton
        lateinit var browserBtn: ImageButton
        lateinit var shareBtn: ImageButton
        lateinit var itemImage: ImageView
        lateinit var sourceImage: ImageView
        lateinit var title: TextView
        lateinit var sourceTitleAndDate: TextView

        init {
            mView.setCardBackgroundColor(appColors.cardBackground)
            handleClickListeners()
            handleCustomTabActions()
        }

        private fun handleClickListeners() {
            sourceImage = mView.findViewById(R.id.sourceImage)
            itemImage = mView.findViewById(R.id.itemImage)
            title = mView.findViewById(R.id.title)
            sourceTitleAndDate = mView.findViewById(R.id.sourceTitleAndDate)
            saveBtn = mView.findViewById(R.id.favButton)
            shareBtn = mView.findViewById(R.id.shareBtn)
            browserBtn = mView.findViewById(R.id.browserBtn)

            if (!fullHeightCards) {
                itemImage.maxHeight = c.resources.getDimension(R.dimen.card_image_max_height).toInt()
                itemImage.scaleType = ScaleType.CENTER_CROP
            }

            saveBtn.setOnLikeListener(object : OnLikeListener {
                override fun liked(likeButton: LikeButton) {
                    val (id) = items[adapterPosition]
                    api.starrItem(id).enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {}

                        override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                            saveBtn.isLiked = false
                            Toast.makeText(c, R.string.cant_mark_favortie, Toast.LENGTH_SHORT).show()
                        }
                    })
                }

                override fun unLiked(likeButton: LikeButton) {
                    val (id) = items[adapterPosition]
                    api.unstarrItem(id).enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(call: Call<SuccessResponse>, response: Response<SuccessResponse>) {}

                        override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                            saveBtn.isLiked = true
                            Toast.makeText(c, R.string.cant_unmark_favortie, Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            })

            shareBtn.setOnClickListener {
                c.shareLink(items[adapterPosition].getLinkDecoded())
            }

            browserBtn.setOnClickListener {
                c.openInBrowserAsNewTask(items[adapterPosition])
            }
        }

        private fun handleCustomTabActions() {
            val customTabsIntent = c.buildCustomTabsIntent()
            helper.bindCustomTabsService(app)

            mView.setOnClickListener {
                c.openItemUrl(items[adapterPosition].getLinkDecoded(),
                    customTabsIntent,
                    internalBrowser,
                    articleViewer,
                    app)
            }
        }
    }
}
