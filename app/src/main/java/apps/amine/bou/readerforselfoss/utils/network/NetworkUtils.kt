package apps.amine.bou.readerforselfoss.utils.network

import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.View
import android.widget.TextView
import apps.amine.bou.readerforselfoss.R
import com.google.android.material.snackbar.Snackbar

var snackBarShown = false
var view: View? = null
lateinit var s: Snackbar

fun Context.isNetworkAccessible(v: View?, overrideOffline: Boolean = false): Boolean {
    val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
    val networkIsAccessible = activeNetwork != null && activeNetwork.isConnectedOrConnecting

    if (v != null && (!networkIsAccessible || overrideOffline) && (!snackBarShown || v != view)) {
        view = v
        s = Snackbar
            .make(
                v,
                R.string.no_network_connectivity,
                Snackbar.LENGTH_INDEFINITE
            )

        s.setAction(android.R.string.ok) {
            snackBarShown = false
            s.dismiss()
        }

        val view = s.view
        val tv: TextView = view.findViewById(com.google.android.material.R.id.snackbar_text)
        tv.setTextColor(Color.WHITE)
        s.show()
        snackBarShown = true
    }
    if (snackBarShown && networkIsAccessible && !overrideOffline) {
        s.dismiss()
    }
    return if(overrideOffline) overrideOffline else networkIsAccessible
}