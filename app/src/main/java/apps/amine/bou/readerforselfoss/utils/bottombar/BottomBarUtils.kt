package apps.amine.bou.readerforselfoss.utils.bottombar

import com.ashokvarma.bottomnavigation.TextBadgeItem

fun TextBadgeItem.removeBadge(): TextBadgeItem {
    this.setText("")
    this.hide()
    return this
}

fun TextBadgeItem.maybeShow(): TextBadgeItem =
        if (this.isHidden) this.show() else this
