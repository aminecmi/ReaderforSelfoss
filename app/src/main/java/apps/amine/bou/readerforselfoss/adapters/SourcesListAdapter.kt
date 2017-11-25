package apps.amine.bou.readerforselfoss.adapters

import android.app.Activity
import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.Sources
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.utils.glide.circularBitmapDrawable
import apps.amine.bou.readerforselfoss.utils.toTextDrawableString
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import kotlinx.android.synthetic.main.source_list_item.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SourcesListAdapter(
        private val app: Activity,
        private val items: ArrayList<Sources>,
        private val api: SelfossApi
) : RecyclerView.Adapter<SourcesListAdapter.ViewHolder>() {
    private val c: Context = app.baseContext
    private val generator: ColorGenerator = ColorGenerator.MATERIAL

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(c).inflate(
                R.layout.source_list_item,
                parent,
                false
        ) as ConstraintLayout
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itm = items[position]

        if (itm.getIcon(c).isEmpty()) {
            val color = generator.getColor(itm.title)

            val drawable =
                    TextDrawable
                            .builder()
                            .round()
                            .build(itm.title.toTextDrawableString(), color)
            holder.mView.itemImage.setImageDrawable(drawable)
        } else {
            c.circularBitmapDrawable(itm.getIcon(c), holder.mView.itemImage)
        }

        holder.mView.sourceTitle.text = itm.title
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(internal val mView: ConstraintLayout) : RecyclerView.ViewHolder(mView) {

        init {
            handleClickListeners()
        }

        private fun handleClickListeners() {

            val deleteBtn: Button = mView.findViewById(R.id.deleteBtn)

            deleteBtn.setOnClickListener {
                val (id) = items[adapterPosition]
                api.deleteSource(id).enqueue(object : Callback<SuccessResponse> {
                    override fun onResponse(
                            call: Call<SuccessResponse>,
                            response: Response<SuccessResponse>
                    ) {
                        if (response.body() != null && response.body()!!.isSuccess) {
                            items.removeAt(adapterPosition)
                            notifyItemRemoved(adapterPosition)
                            notifyItemRangeChanged(adapterPosition, itemCount)
                        } else {
                            Toast.makeText(
                                    app,
                                    R.string.can_delete_source,
                                    Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                        Toast.makeText(
                                app,
                                R.string.can_delete_source,
                                Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }
        }
    }
}
