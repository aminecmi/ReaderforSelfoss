/* From https://github.com/mikepenz/MaterialDrawer/blob/develop/app/src/main/java/com/mikepenz/materialdrawer/app/drawerItems/CustomBaseViewHolder.java */
package apps.amine.bou.readerforselfoss.utils.drawer

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import apps.amine.bou.readerforselfoss.R

open class CustomBaseViewHolder(var view: View) : RecyclerView.ViewHolder(view) {
    var icon: ImageView = view.findViewById(R.id.material_drawer_icon)
    var name: TextView = view.findViewById(R.id.material_drawer_name)
    var description: TextView = view.findViewById(R.id.material_drawer_description)
}
