/* From https://github.com/mikepenz/MaterialDrawer/blob/develop/app/src/main/java/com/mikepenz/materialdrawer/app/drawerItems/CustomUrlBasePrimaryDrawerItem.java */
package apps.amine.bou.readerforselfoss.utils.drawer

import android.net.Uri
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import android.support.v7.widget.RecyclerView

import com.mikepenz.materialdrawer.holder.ColorHolder
import com.mikepenz.materialdrawer.holder.ImageHolder
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.BaseDrawerItem
import com.mikepenz.materialdrawer.util.DrawerImageLoader
import com.mikepenz.materialdrawer.util.DrawerUIUtils
import com.mikepenz.materialize.util.UIUtils

abstract class CustomUrlBasePrimaryDrawerItem<T, VH : RecyclerView.ViewHolder> : BaseDrawerItem<T, VH>() {
    fun withIcon(url: String): T {
        this.icon = ImageHolder(url)
        return this as T
    }

    fun withIcon(uri: Uri): T {
        this.icon = ImageHolder(uri)
        return this as T
    }

    var description: StringHolder? = null
        private set
    var descriptionTextColor: ColorHolder? = null
        private set

    fun withDescription(description: String): T {
        this.description = StringHolder(description)
        return this as T
    }

    fun withDescription(@StringRes descriptionRes: Int): T {
        this.description = StringHolder(descriptionRes)
        return this as T
    }

    fun withDescriptionTextColor(@ColorInt color: Int): T {
        this.descriptionTextColor = ColorHolder.fromColor(color)
        return this as T
    }

    fun withDescriptionTextColorRes(@ColorRes colorRes: Int): T {
        this.descriptionTextColor = ColorHolder.fromColorRes(colorRes)
        return this as T
    }

    /**
     * a helper method to have the logic for all secondaryDrawerItems only once

     * @param viewHolder
     */
    protected fun bindViewHelper(viewHolder: CustomBaseViewHolder) {
        val ctx = viewHolder.itemView.context

        //set the identifier from the drawerItem here. It can be used to run tests
        viewHolder.itemView.id = hashCode()

        //set the item selected if it is
        viewHolder.itemView.isSelected = isSelected

        //get the correct color for the background
        val selectedColor = getSelectedColor(ctx)
        //get the correct color for the text
        val color = getColor(ctx)
        val selectedTextColor = getSelectedTextColor(ctx)
        //get the correct color for the icon
        val iconColor = getIconColor(ctx)
        val selectedIconColor = getSelectedIconColor(ctx)

        //set the background for the item
        UIUtils.setBackground(
                viewHolder.view,
                UIUtils.getSelectableBackground(ctx, selectedColor, true)
        )
        //set the text for the name
        StringHolder.applyTo(this.getName(), viewHolder.name)
        //set the text for the description or hide
        StringHolder.applyToOrHide(this.description, viewHolder.description)

        //set the colors for textViews
        viewHolder.name.setTextColor(getTextColorStateList(color, selectedTextColor))
        //set the description text color
        ColorHolder.applyToOr(
                descriptionTextColor,
                viewHolder.description,
                getTextColorStateList(color, selectedTextColor)
        )

        //define the typeface for our textViews
        if (getTypeface() != null) {
            viewHolder.name.typeface = getTypeface()
            viewHolder.description.typeface = getTypeface()
        }

        //we make sure we reset the image first before setting the new one in case there is an empty one
        DrawerImageLoader.getInstance().cancelImage(viewHolder.icon)
        viewHolder.icon.setImageBitmap(null)
        //get the drawables for our icon and set it
        ImageHolder.applyTo(icon, viewHolder.icon, "customUrlItem")

        //for android API 17 --> Padding not applied via xml
        DrawerUIUtils.setDrawerVerticalPadding(viewHolder.view)
    }
}
