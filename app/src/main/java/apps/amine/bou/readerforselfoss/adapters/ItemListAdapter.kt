package apps.amine.bou.readerforselfoss.adapters

import android.app.Activity
import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import android.text.Spannable
import android.text.style.ClickableSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.persistence.database.AppDatabase
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.LinkOnTouchListener
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
    override val db: AppDatabase,
    private val helper: CustomTabActivityHelper,
    private val internalBrowser: Boolean,
    private val articleViewer: Boolean,
    override val userIdentifier: String,
    override val appColors: AppColors,
    override val config: Config,
    override val updateItems: (ArrayList<Item>) -> Unit
) : ItemsAdapter<ItemListAdapter.ViewHolder>() {
    private val generator: ColorGenerator = ColorGenerator.MATERIAL
    private val c: Context = app.baseContext

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


        holder.mView.title.text = itm.getTitleDecoded()

        holder.mView.title.setOnTouchListener(LinkOnTouchListener())

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
                c.circularBitmapDrawable(config, itm.getIcon(c), holder.mView.itemImage)
            }
        } else {
            c.bitmapCenterCrop(config, itm.getThumbnail(c), holder.mView.itemImage)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val mView: ConstraintLayout) : RecyclerView.ViewHolder(mView) {

        init {
            handleCustomTabActions()
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
