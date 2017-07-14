package apps.amine.bou.readerforselfoss.adapters

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.TextView
import android.widget.Toast

import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.like.LikeButton
import com.like.OnLikeListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.utils.*
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper

class ItemCardAdapter(private val app: Activity,
                      private val items: ArrayList<Item>,
                      private val api: SelfossApi,
                      private val helper: CustomTabActivityHelper,
                      private val internalBrowser: Boolean,
                      private val articleViewer: Boolean,
                      private val fullHeightCards: Boolean,
                      private val appColors: AppColors) : RecyclerView.Adapter<ItemCardAdapter.ViewHolder>() {
    private val c: Context = app.baseContext
    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.card_item, parent, false) as ConstraintLayout
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itm = items[position]


        holder.saveBtn.isLiked = itm.starred
        holder.title.text = Html.fromHtml(itm.title)

        holder.sourceTitleAndDate.text = itm.sourceAndDateText()

        if (itm.getThumbnail(c).isEmpty()) {
            Glide.clear(holder.itemImage)
            holder.itemImage.setImageDrawable(null)
        } else {
            if (fullHeightCards) {
                c.bitmapFitCenter(itm.getThumbnail(c), holder.itemImage)
            } else {
                c.bitmapCenterCrop(itm.getThumbnail(c), holder.itemImage)
            }
        }

        val fHolder = holder
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

                doUnmark(i, position)
            }

            override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                Toast.makeText(app, app.getString(R.string.cant_mark_read), Toast.LENGTH_SHORT).show()
                items.add(i)
                notifyItemInserted(position)
            }
        })

    }

    inner class ViewHolder(val mView: ConstraintLayout) : RecyclerView.ViewHolder(mView) {
        lateinit var saveBtn: LikeButton
        lateinit var browserBtn: ImageButton
        lateinit var shareBtn: ImageButton
        lateinit var itemImage: ImageView
        lateinit var sourceImage: ImageView
        lateinit var title: TextView
        lateinit var sourceTitleAndDate: TextView

        init {
            (mView.findViewById<CardView>(R.id.card)).setCardBackgroundColor(appColors.cardBackground)
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
                c.openInBrowser(items[adapterPosition])
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
