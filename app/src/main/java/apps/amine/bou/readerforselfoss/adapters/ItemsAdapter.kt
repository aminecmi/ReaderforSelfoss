package apps.amine.bou.readerforselfoss.adapters

import android.app.Activity
import android.graphics.Color
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.Toast
import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.api.selfoss.SelfossApi
import apps.amine.bou.readerforselfoss.api.selfoss.SuccessResponse
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.utils.maybeHandleSilentException
import apps.amine.bou.readerforselfoss.utils.succeeded
import org.acra.ACRA
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

abstract class ItemsAdapter<VH : RecyclerView.ViewHolder?> : RecyclerView.Adapter<VH>() {
    abstract var items: ArrayList<Item>
    abstract val api: SelfossApi
    abstract val debugReadingItems: Boolean
    abstract val userIdentifier: String
    abstract val app: Activity
    abstract val appColors: AppColors
    abstract val updateItems: (ArrayList<Item>) -> Unit

    fun updateAllItems(newItems: ArrayList<Item>) {
        items = newItems
        notifyDataSetChanged()
        updateItems(items)
    }

    private fun doUnmark(i: Item, position: Int) {
        val s = Snackbar
            .make(
                app.findViewById(R.id.coordLayout),
                R.string.marked_as_read,
                Snackbar.LENGTH_LONG
            )
            .setAction(R.string.undo_string) {
                items.add(position, i)
                notifyItemInserted(position)
                updateItems(items)

                api.unmarkItem(i.id).enqueue(object : Callback<SuccessResponse> {
                    override fun onResponse(
                        call: Call<SuccessResponse>,
                        response: Response<SuccessResponse>
                    ) {
                    }

                    override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                        items.remove(i)
                        notifyItemRemoved(position)
                        updateItems(items)
                        doUnmark(i, position)
                    }
                })
            }

        val view = s.view
        val tv: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
        tv.setTextColor(Color.WHITE)
        s.show()
    }

    fun removeItemAtIndex(position: Int) {

        val i = items[position]

        items.remove(i)
        notifyItemRemoved(position)
        updateItems(items)


        api.markItem(i.id).enqueue(object : Callback<SuccessResponse> {
            override fun onResponse(
                call: Call<SuccessResponse>,
                response: Response<SuccessResponse>
            ) {
                if (!response.succeeded() && debugReadingItems) {
                    val message =
                        "message: ${response.message()} " +
                                "response isSuccess: ${response.isSuccessful} " +
                                "response code: ${response.code()} " +
                                "response message: ${response.message()} " +
                                "response errorBody: ${response.errorBody()?.string()} " +
                                "body success: ${response.body()?.success} " +
                                "body isSuccess: ${response.body()?.isSuccess}"
                    ACRA.getErrorReporter().maybeHandleSilentException(Exception(message), app)
                    Toast.makeText(app.baseContext, message, Toast.LENGTH_LONG).show()
                }
                doUnmark(i, position)
            }

            override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                if (debugReadingItems) {
                    ACRA.getErrorReporter().maybeHandleSilentException(t, app)
                    Toast.makeText(app.baseContext, t.message, Toast.LENGTH_LONG).show()
                }
                Toast.makeText(
                    app,
                    app.getString(R.string.cant_mark_read),
                    Toast.LENGTH_SHORT
                ).show()
                items.add(i)
                notifyItemInserted(position)
                updateItems(items)

            }
        })
    }

    fun addItemAtIndex(item: Item, position: Int) {
        items.add(position, item)
        notifyItemInserted(position)
        updateItems(items)

    }

    fun addItemsAtEnd(newItems: List<Item>) {
        val oldSize = items.size
        items.addAll(newItems)
        notifyItemRangeInserted(oldSize, newItems.size)
        updateItems(items)

    }
}