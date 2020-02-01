package apps.amine.bou.readerforselfoss.adapters

import android.app.Activity
import android.content.Context
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView.ScaleType
import android.widget.Toast
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.persistence.database.AppDatabase
import apps.amine.bou.readerforselfoss.persistence.entities.ActionEntity
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.LinkOnTouchListener
import apps.amine.bou.readerforselfoss.utils.buildCustomTabsIntent
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper
import apps.amine.bou.readerforselfoss.utils.glide.bitmapCenterCrop
import apps.amine.bou.readerforselfoss.utils.glide.circularBitmapDrawable
import apps.amine.bou.readerforselfoss.utils.network.isNetworkAccessible
import apps.amine.bou.readerforselfoss.utils.openInBrowserAsNewTask
import apps.amine.bou.readerforselfoss.utils.openItemUrl
import apps.amine.bou.readerforselfoss.utils.shareLink
import apps.amine.bou.readerforselfoss.utils.sourceAndDateText
import apps.amine.bou.readerforselfoss.utils.toTextDrawableString
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.Glide
import com.like.LikeButton
import com.like.OnLikeListener
import kotlinx.android.synthetic.main.card_item.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

class ItemCardAdapter(
    override val app: Activity,
    override var items: ArrayList<Item>,
    override val api: SelfossApi,
    override val db: AppDatabase,
    private val helper: CustomTabActivityHelper,
    private val internalBrowser: Boolean,
    private val articleViewer: Boolean,
    private val fullHeightCards: Boolean,
    override val appColors: AppColors,
    override val userIdentifier: String,
    override val config: Config,
    override val updateItems: (ArrayList<Item>) -> Unit
) : ItemsAdapter<ItemCardAdapter.ViewHolder>() {
    private val c: Context = app.baseContext
    private val generator: ColorGenerator = ColorGenerator.MATERIAL
    private val imageMaxHeight: Int =
        c.resources.getDimension(R.dimen.card_image_max_height).toInt()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.card_item, parent, false) as CardView
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itm = items[position]


        holder.mView.favButton.isLiked = itm.starred
        holder.mView.title.text = Html.fromHtml(itm.title)
        holder.mView.title.setOnTouchListener(LinkOnTouchListener())

        holder.mView.title.setLinkTextColor(appColors.colorAccent)

        holder.mView.sourceTitleAndDate.text = itm.sourceAndDateText()

        if (!fullHeightCards) {
            holder.mView.itemImage.maxHeight = imageMaxHeight
            holder.mView.itemImage.scaleType = ScaleType.CENTER_CROP
        }

        if (itm.getThumbnail(c).isEmpty()) {
            holder.mView.itemImage.visibility = View.GONE
            Glide.with(c).clear(holder.mView.itemImage)
            holder.mView.itemImage.setImageDrawable(null)
        } else {
            holder.mView.itemImage.visibility = View.VISIBLE
            c.bitmapCenterCrop(config, itm.getThumbnail(c), holder.mView.itemImage)
        }

        if (itm.getIcon(c).isEmpty()) {
            val color = generator.getColor(itm.sourcetitle)

            val drawable =
                TextDrawable
                    .builder()
                    .round()
                    .build(itm.sourcetitle.toTextDrawableString(c), color)
            holder.mView.sourceImage.setImageDrawable(drawable)
        } else {
            c.circularBitmapDrawable(config, itm.getIcon(c), holder.mView.sourceImage)
        }

        holder.mView.favButton.isLiked = itm.starred
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(val mView: CardView) : RecyclerView.ViewHolder(mView) {
        init {
            mView.setCardBackgroundColor(appColors.cardBackgroundColor)
            handleClickListeners()
            handleCustomTabActions()
        }

        private fun handleClickListeners() {

            mView.favButton.setOnLikeListener(object : OnLikeListener {
                override fun liked(likeButton: LikeButton) {
                    val (id) = items[adapterPosition]
                    if (c.isNetworkAccessible(null)) {
                        api.starrItem(id).enqueue(object : Callback<SuccessResponse> {
                            override fun onResponse(
                                call: Call<SuccessResponse>,
                                response: Response<SuccessResponse>
                            ) {
                            }

                            override fun onFailure(
                                call: Call<SuccessResponse>,
                                t: Throwable
                            ) {
                                mView.favButton.isLiked = false
                                Toast.makeText(
                                    c,
                                    R.string.cant_mark_favortie,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    } else {
                        thread {
                            db.actionsDao().insertAllActions(ActionEntity(id, false, false, true, false))
                        }
                    }
                }

                override fun unLiked(likeButton: LikeButton) {
                    val (id) = items[adapterPosition]
                    if (c.isNetworkAccessible(null)) {
                        api.unstarrItem(id).enqueue(object : Callback<SuccessResponse> {
                            override fun onResponse(
                                call: Call<SuccessResponse>,
                                response: Response<SuccessResponse>
                            ) {
                            }

                            override fun onFailure(
                                call: Call<SuccessResponse>,
                                t: Throwable
                            ) {
                                mView.favButton.isLiked = true
                                Toast.makeText(
                                    c,
                                    R.string.cant_unmark_favortie,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                    } else {
                        thread {
                            db.actionsDao().insertAllActions(ActionEntity(id, false, false, false, true))
                        }
                    }
                }
            })

            mView.shareBtn.setOnClickListener {
                val item = items[adapterPosition]
                c.shareLink(item.getLinkDecoded(), item.title)
            }

            mView.browserBtn.setOnClickListener {
                c.openInBrowserAsNewTask(items[adapterPosition])
            }
        }

        private fun handleCustomTabActions() {
            val customTabsIntent = c.buildCustomTabsIntent()
            helper.bindCustomTabsService(app)

            mView.setOnClickListener {
                c.openItemUrl(
                    items,
                    adapterPosition,
                    items[adapterPosition].getLinkDecoded(),
                    customTabsIntent,
                    internalBrowser,
                    articleViewer,
                    app
                )
            }
        }
    }
}
