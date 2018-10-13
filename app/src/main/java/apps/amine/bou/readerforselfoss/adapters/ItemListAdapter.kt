package apps.amine.bou.readerforselfoss.adapters

import android.app.Activity
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import android.text.Html
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import apps.amine.bou.readerforselfoss.utils.toTextDrawableString
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.like.LikeButton
import com.like.OnLikeListener
import kotlinx.android.synthetic.main.list_item.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class ItemListAdapter(
    override val app: Activity,
    override var items: ArrayList<Item>,
    override val api: SelfossApi,
    private val helper: CustomTabActivityHelper,
    private val internalBrowser: Boolean,
    private val articleViewer: Boolean,
    override val debugReadingItems: Boolean,
    override val userIdentifier: String,
    override val appColors: AppColors,
    override val updateItems: (ArrayList<Item>) -> Unit
) : ItemsAdapter<ItemListAdapter.ViewHolder>() {
    private val generator: ColorGenerator = ColorGenerator.MATERIAL
    private val c: Context = app.baseContext
    private val bars: ArrayList<Boolean> = ArrayList(Collections.nCopies(items.size + 1, false))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(c).inflate(
            R.layout.list_item,
            parent,
            false
        ) as ConstraintLayout
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itm = items[position]


        holder.mView.title.text = Html.fromHtml(itm.title)

        holder.mView.title.setLinkTextColor(appColors.colorAccent)

        holder.mView.sourceTitleAndDate.text = itm.sourceAndDateText()

        if (itm.getThumbnail(c).isEmpty()) {
            val sizeInInt = 46
            val sizeInDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, sizeInInt.toFloat(), c.resources
                    .displayMetrics
            ).toInt()

            val marginInInt = 16
            val marginInDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, marginInInt.toFloat(), c.resources
                    .displayMetrics
            ).toInt()

            val params = holder.mView.itemImage.layoutParams as ViewGroup.MarginLayoutParams
            params.height = sizeInDp
            params.width = sizeInDp
            params.setMargins(marginInDp, 0, 0, 0)
            holder.mView.itemImage.layoutParams = params

            if (itm.getIcon(c).isEmpty()) {
                val color = generator.getColor(itm.sourcetitle)

                val drawable =
                    TextDrawable
                        .builder()
                        .round()
                        .build(itm.sourcetitle.toTextDrawableString(c), color)

                holder.mView.itemImage.setImageDrawable(drawable)
            } else {
                c.circularBitmapDrawable(itm.getIcon(c), holder.mView.itemImage)
            }
        } else {
            c.bitmapCenterCrop(itm.getThumbnail(c), holder.mView.itemImage)
        }

        // TODO: maybe handle this differently. It crashes when changing tab
        try {
            if (bars[position]) {
                holder.mView.actionBar.visibility = View.VISIBLE
            } else {
                holder.mView.actionBar.visibility = View.GONE
            }
        } catch (e: IndexOutOfBoundsException) {
            holder.mView.actionBar.visibility = View.GONE
        }

        holder.mView.favButton.isLiked = itm.starred
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val mView: ConstraintLayout) : RecyclerView.ViewHolder(mView) {

        init {
            handleClickListeners()
            handleCustomTabActions()
        }

        private fun handleClickListeners() {

            mView.favButton.setOnLikeListener(object : OnLikeListener {
                override fun liked(likeButton: LikeButton) {
                    val (id) = items[adapterPosition]
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
                }

                override fun unLiked(likeButton: LikeButton) {
                    val (id) = items[adapterPosition]
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
                }
            })

            mView.shareBtn.setOnClickListener {
                c.shareLink(items[adapterPosition].getLinkDecoded())
            }

            mView.browserBtn.setOnClickListener {
                c.openInBrowserAsNewTask(items[adapterPosition])

            }
        }

        private fun handleCustomTabActions() {
            val customTabsIntent = c.buildCustomTabsIntent()
            helper.bindCustomTabsService(app)

            mView.setOnClickListener { actionBarShowHide() }
            mView.setOnLongClickListener {
                c.openItemUrl(
                    items,
                    adapterPosition,
                    items[adapterPosition].getLinkDecoded(),
                    customTabsIntent,
                    internalBrowser,
                    articleViewer,
                    app
                )
                true
            }
        }

        private fun actionBarShowHide() {
            bars[adapterPosition] = true
            if (mView.actionBar.visibility == View.GONE) {
                mView.actionBar.visibility = View.VISIBLE
            } else {
                mView.actionBar.visibility = View.GONE
            }
        }
    }
}
