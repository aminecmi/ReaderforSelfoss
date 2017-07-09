package apps.amine.bou.readerforselfoss.adapters


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.support.constraint.ConstraintLayout
import android.support.design.widget.Snackbar
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory
import android.support.v7.widget.RecyclerView
import android.text.Html
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

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


class ItemListAdapter(private val app: Activity,
                      private val items: ArrayList<Item>,
                      private val api: SelfossApi,
                      private val helper: CustomTabActivityHelper,
                      private val clickBehavior: Boolean,
                      private val internalBrowser: Boolean,
                      private val articleViewer: Boolean) : RecyclerView.Adapter<ItemListAdapter.ViewHolder>() {
    private val generator: ColorGenerator = ColorGenerator.MATERIAL
    private val c: Context = app.baseContext
    private val bars: ArrayList<Boolean> = ArrayList(Collections.nCopies(items.size + 1, false))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(c).inflate(R.layout.list_item, parent, false) as ConstraintLayout
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itm = items[position]


        holder.saveBtn.isLiked = itm.starred
        holder.title.text = Html.fromHtml(itm.title)

        holder.sourceTitleAndDate.text = itm.sourceAndDateText()

        if (itm.getThumbnail(c).isEmpty()) {
            val sizeInInt = 46
            val sizeInDp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, sizeInInt.toFloat(), c.resources
                .displayMetrics).toInt()

            val marginInInt = 16
            val marginInDp = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, marginInInt.toFloat(), c.resources
                    .displayMetrics).toInt()

            val params = holder.sourceImage.layoutParams as ViewGroup.MarginLayoutParams
            params.height = sizeInDp
            params.width = sizeInDp
            params.setMargins(marginInDp, 0, 0, 0)
            holder.sourceImage.layoutParams = params

            if (itm.getIcon(c).isEmpty()) {
                val color = generator.getColor(itm.sourcetitle)
                val textDrawable = StringBuilder()
                for (s in itm.sourcetitle.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                    textDrawable.append(s[0])
                }

                val builder = TextDrawable.builder().round()

                val drawable = builder.build(textDrawable.toString(), color)
                holder.sourceImage.setImageDrawable(drawable)
            } else {
                c.circularBitmapDrawable(itm.getIcon(c), holder.sourceImage)
            }
        } else {
            c.bitmapCenterCrop(itm.getThumbnail(c), holder.sourceImage)
        }

        if (bars[position]) holder.actionBar.visibility = View.VISIBLE else holder.actionBar.visibility = View.GONE

        holder.saveBtn.isLiked = itm.starred
    }

    override fun getItemCount(): Int = items.size


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
        lateinit var actionBar: RelativeLayout
        lateinit var sourceImage: ImageView
        lateinit var title: TextView
        lateinit var sourceTitleAndDate: TextView

        init {
            handleClickListeners()
            handleCustomTabActions()
        }

        private fun handleClickListeners() {
            actionBar = mView.findViewById(R.id.actionBar)
            sourceImage = mView.findViewById(R.id.itemImage)
            title = mView.findViewById(R.id.title)
            sourceTitleAndDate = mView.findViewById(R.id.sourceTitleAndDate)
            saveBtn = mView.findViewById(R.id.favButton)
            shareBtn = mView.findViewById(R.id.shareBtn)
            browserBtn = mView.findViewById(R.id.browserBtn)


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


            if (!clickBehavior) {
                mView.setOnClickListener {
                    c.openItemUrl(items[adapterPosition],
                        customTabsIntent,
                        internalBrowser,
                        articleViewer,
                        app)
                }
                mView.setOnLongClickListener {
                    actionBarShowHide()
                    true
                }
            } else {
                mView.setOnClickListener { actionBarShowHide() }
                mView.setOnLongClickListener {
                    c.openItemUrl(items[adapterPosition],
                        customTabsIntent,
                        internalBrowser,
                        articleViewer,
                        app)
                    true
                }
            }
        }

        private fun actionBarShowHide() {
            bars[adapterPosition] = true
            if (actionBar.visibility == View.GONE) actionBar.visibility = View.VISIBLE else actionBar.visibility = View.GONE
        }
    }
}
