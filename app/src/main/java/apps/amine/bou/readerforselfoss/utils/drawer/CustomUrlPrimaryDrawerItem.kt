/* From https://github.com/mikepenz/MaterialDrawer/blob/develop/app/src/main/java/com/mikepenz/materialdrawer/app/drawerItems/CustomUrlPrimaryDrawerItem.java */
package apps.amine.bou.readerforselfoss.utils.drawer

import android.support.annotation.LayoutRes
import android.support.annotation.StringRes
import android.view.View
import android.widget.TextView
import apps.amine.bou.readerforselfoss.R
import com.mikepenz.materialdrawer.holder.BadgeStyle
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.interfaces.ColorfulBadgeable

class CustomUrlPrimaryDrawerItem : CustomUrlBasePrimaryDrawerItem<CustomUrlPrimaryDrawerItem, CustomUrlPrimaryDrawerItem.ViewHolder>(), ColorfulBadgeable<CustomUrlPrimaryDrawerItem> {
    protected var mBadge: StringHolder = StringHolder("")
    protected var mBadgeStyle = BadgeStyle()

    override fun withBadge(badge: StringHolder): CustomUrlPrimaryDrawerItem {
        this.mBadge = badge
        return this
    }

    override fun withBadge(badge: String): CustomUrlPrimaryDrawerItem {
        this.mBadge = StringHolder(badge)
        return this
    }

    override fun withBadge(@StringRes badgeRes: Int): CustomUrlPrimaryDrawerItem {
        this.mBadge = StringHolder(badgeRes)
        return this
    }

    override fun withBadgeStyle(badgeStyle: BadgeStyle): CustomUrlPrimaryDrawerItem {
        this.mBadgeStyle = badgeStyle
        return this
    }

    override fun getBadge(): StringHolder {
        return mBadge
    }

    override fun getBadgeStyle(): BadgeStyle {
        return mBadgeStyle
    }

    override fun getType(): Int {
        return R.id.material_drawer_item_custom_url_item
    }

    @LayoutRes
    override fun getLayoutRes(): Int {
        return R.layout.material_drawer_item_primary
    }

    override fun bindView(viewHolder: ViewHolder, payloads: List<*>?) {
        super.bindView(viewHolder, payloads)

        val ctx = viewHolder.itemView.context

        //bind the basic view parts
        bindViewHelper(viewHolder)

        //set the text for the badge or hide
        val badgeVisible = StringHolder.applyToOrHide(mBadge, viewHolder.badge)
        //style the badge if it is visible
        if (badgeVisible) {
            mBadgeStyle.style(
                    viewHolder.badge,
                    getTextColorStateList(getColor(ctx), getSelectedTextColor(ctx))
            )
            viewHolder.badgeContainer.visibility = View.VISIBLE
        } else {
            viewHolder.badgeContainer.visibility = View.GONE
        }

        //define the typeface for our textViews
        if (getTypeface() != null) {
            viewHolder.badge.typeface = getTypeface()
        }

        //call the onPostBindView method to trigger post bind view actions (like the listener to modify the item if required)
        onPostBindView(this, viewHolder.itemView)
    }

    override fun getViewHolder(v: View): ViewHolder {
        return ViewHolder(v)
    }

    class ViewHolder(view: View) : CustomBaseViewHolder(view) {
        val badgeContainer: View = view.findViewById(R.id.material_drawer_badge_container)
        val badge: TextView = view.findViewById(R.id.material_drawer_badge)
    }
}