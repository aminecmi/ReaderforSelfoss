package apps.amine.bou.readerforselfoss.adapters

import android.app.Activity
import android.content.Context
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView.ScaleType
import android.widget.Toast
import androidx.core.content.ContextCompat
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.databinding.CardItemBinding
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
        val binding = CardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder) {
            val itm = items[position]


            binding.favButton.isLiked = itm.starred
            binding.title.text = itm.getTitleDecoded()
            binding.title.setTextColor(ContextCompat.getColor(
                    c,
                    appColors.textColor
            ))
            binding.title.setOnTouchListener(LinkOnTouchListener())

            binding.title.setLinkTextColor(appColors.colorAccent)

            binding.sourceTitleAndDate.text = itm.sourceAndDateText()

            binding.sourceTitleAndDate.setTextColor(ContextCompat.getColor(
                    c,
                    appColors.textColor
            ))

            if (!fullHeightCards) {
                binding.itemImage.maxHeight = imageMaxHeight
                binding.itemImage.scaleType = ScaleType.CENTER_CROP
            }

            if (itm.getThumbnail(c).isEmpty()) {
                binding.itemImage.visibility = View.GONE
                Glide.with(c).clear(binding.itemImage)
                binding.itemImage.setImageDrawable(null)
            } else {
                binding.itemImage.visibility = View.VISIBLE
                c.bitmapCenterCrop(config, itm.getThumbnail(c), binding.itemImage)
            }

            if (itm.getIcon(c).isEmpty()) {
                val color = generator.getColor(itm.getSourceTitle())

                val drawable =
                        TextDrawable
                                .builder()
                                .round()
                                .build(itm.getSourceTitle().toTextDrawableString(c), color)
                binding.sourceImage.setImageDrawable(drawable)
            } else {
                c.circularBitmapDrawable(config, itm.getIcon(c), binding.sourceImage)
            }

            binding.favButton.isLiked = itm.starred
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    inner class ViewHolder(val binding: CardItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setCardBackgroundColor(appColors.cardBackgroundColor)
            handleClickListeners()
            handleCustomTabActions()
        }

        private fun handleClickListeners() {

            binding.favButton.setOnLikeListener(object : OnLikeListener {
                override fun liked(likeButton: LikeButton) {
                    val (id) = items[bindingAdapterPosition]
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
                                binding.favButton.isLiked = false
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
                    val (id) = items[bindingAdapterPosition]
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
                                binding.favButton.isLiked = true
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

            binding.shareBtn.setOnClickListener {
                val item = items[bindingAdapterPosition]
                c.shareLink(item.getLinkDecoded(), item.getTitleDecoded())
            }

            binding.browserBtn.setOnClickListener {
                c.openInBrowserAsNewTask(items[bindingAdapterPosition])
            }
        }

        private fun handleCustomTabActions() {
            val customTabsIntent = c.buildCustomTabsIntent()
            helper.bindCustomTabsService(app)

            binding.root.setOnClickListener {
                c.openItemUrl(
                    items,
                    bindingAdapterPosition,
                    items[bindingAdapterPosition].getLinkDecoded(),
                    customTabsIntent,
                    internalBrowser,
                    articleViewer,
                    app
                )
            }
        }
    }
}
