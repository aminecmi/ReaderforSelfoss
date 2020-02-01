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
import apps.amine.bou.readerforselfoss.persistence.database.AppDatabase
import apps.amine.bou.readerforselfoss.persistence.entities.ActionEntity
import apps.amine.bou.readerforselfoss.themes.AppColors
import apps.amine.bou.readerforselfoss.utils.Config
import apps.amine.bou.readerforselfoss.utils.network.isNetworkAccessible
import apps.amine.bou.readerforselfoss.utils.persistence.toEntity
import apps.amine.bou.readerforselfoss.utils.succeeded
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.concurrent.thread

abstract class ItemsAdapter<VH : RecyclerView.ViewHolder?> : RecyclerView.Adapter<VH>() {
    abstract var items: ArrayList<Item>
    abstract val api: SelfossApi
    abstract val db: AppDatabase
    abstract val userIdentifier: String
    abstract val app: Activity
    abstract val appColors: AppColors
    abstract val config: Config
    abstract val updateItems: (ArrayList<Item>) -> Unit

    fun updateAllItems(newItems: ArrayList<Item>) {
        items = newItems
        notifyDataSetChanged()
        updateItems(items)
    }

    private fun unmarkSnackbar(i: Item, position: Int) {
        val s = Snackbar
            .make(
                app.findViewById(R.id.coordLayout),
                R.string.marked_as_read,
                Snackbar.LENGTH_LONG
            )
            .setAction(R.string.undo_string) {
                items.add(position, i)
                thread {
                    db.itemsDao().insertAllItems(i.toEntity())
                }
                notifyItemInserted(position)
                updateItems(items)

                if (app.isNetworkAccessible(null)) {
                    api.unmarkItem(i.id).enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(
                            call: Call<SuccessResponse>,
                            response: Response<SuccessResponse>
                        ) {
                        }

                        override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                            items.remove(i)
                            thread {
                                db.itemsDao().delete(i.toEntity())
                            }
                            notifyItemRemoved(position)
                            updateItems(items)
                        }
                    })
                } else {
                    thread {
                        db.actionsDao().insertAllActions(ActionEntity(i.id, false, true, false, false))
                    }
                }
            }

        val view = s.view
        val tv: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
        tv.setTextColor(Color.WHITE)
        s.show()
    }

    private fun markSnackbar(i: Item, position: Int) {
        val s = Snackbar
            .make(
                app.findViewById(R.id.coordLayout),
                R.string.marked_as_unread,
                Snackbar.LENGTH_LONG
            )
            .setAction(R.string.undo_string) {
                items.add(position, i)
                thread {
                    db.itemsDao().delete(i.toEntity())
                }
                notifyItemInserted(position)
                updateItems(items)

                if (app.isNetworkAccessible(null)) {
                    api.markItem(i.id).enqueue(object : Callback<SuccessResponse> {
                        override fun onResponse(
                            call: Call<SuccessResponse>,
                            response: Response<SuccessResponse>
                        ) {
                        }

                        override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                            items.remove(i)
                            thread {
                                db.itemsDao().insertAllItems(i.toEntity())
                            }
                            notifyItemRemoved(position)
                            updateItems(items)
                        }
                    })
                } else {
                    thread {
                        db.actionsDao().insertAllActions(ActionEntity(i.id, true, false, false, false))
                    }
                }
            }

        val view = s.view
        val tv: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
        tv.setTextColor(Color.WHITE)
        s.show()
    }

    fun handleItemAtIndex(position: Int) {
        if (unreadItemStatusAtIndex(position)) {
            readItemAtIndex(position)
        } else {
            unreadItemAtIndex(position)
        }
    }

    fun unreadItemStatusAtIndex(position: Int): Boolean {
        return items[position].unread
    }

    private fun readItemAtIndex(position: Int) {
        val i = items[position]
        items.remove(i)
        notifyItemRemoved(position)
        updateItems(items)

        thread {
            db.itemsDao().delete(i.toEntity())
        }

        if (app.isNetworkAccessible(null)) {
            api.markItem(i.id).enqueue(object : Callback<SuccessResponse> {
                override fun onResponse(
                    call: Call<SuccessResponse>,
                    response: Response<SuccessResponse>
                ) {

                    unmarkSnackbar(i, position)
                }

                override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                    Toast.makeText(
                        app,
                        app.getString(R.string.cant_mark_read),
                        Toast.LENGTH_SHORT
                    ).show()
                    items.add(position, i)
                    notifyItemInserted(position)
                    updateItems(items)

                    thread {
                        db.itemsDao().insertAllItems(i.toEntity())
                    }
                }
            })
        } else {
            thread {
                db.actionsDao().insertAllActions(ActionEntity(i.id, true, false, false, false))
            }
        }
    }

    private fun unreadItemAtIndex(position: Int) {
        val i = items[position]
        items.remove(i)
        notifyItemRemoved(position)
        updateItems(items)

        thread {
            db.itemsDao().insertAllItems(i.toEntity())
        }

        if (app.isNetworkAccessible(null)) {
            api.unmarkItem(i.id).enqueue(object : Callback<SuccessResponse> {
                override fun onResponse(
                    call: Call<SuccessResponse>,
                    response: Response<SuccessResponse>
                ) {

                    markSnackbar(i, position)
                }

                override fun onFailure(call: Call<SuccessResponse>, t: Throwable) {
                    Toast.makeText(
                        app,
                        app.getString(R.string.cant_mark_unread),
                        Toast.LENGTH_SHORT
                    ).show()
                    items.add(i)
                    notifyItemInserted(position)
                    updateItems(items)

                    thread {
                        db.itemsDao().delete(i.toEntity())
                    }
                }
            })
        } else {
            thread {
                db.actionsDao().insertAllActions(ActionEntity(i.id, false, true, false, false))
            }
        }
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