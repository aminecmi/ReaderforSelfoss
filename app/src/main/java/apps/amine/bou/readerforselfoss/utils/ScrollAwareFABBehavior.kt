package apps.amine.bou.readerforselfoss.utils

import android.content.Context
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.util.AttributeSet
import android.view.View

class ScrollAwareFABBehavior(
    context: Context,
    attrs: AttributeSet
) : androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior<FloatingActionButton>() {


    override fun onStartNestedScroll(
        coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout,
        child: FloatingActionButton,
        directTargetChild: View,
        target: View,
        nestedScrollAxes: Int
    ): Boolean {
        return true
    }

    override fun onNestedScroll(
        coordinatorLayout: androidx.coordinatorlayout.widget.CoordinatorLayout,
        child: FloatingActionButton,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        super.onNestedScroll(
            coordinatorLayout,
            child,
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed
        )
        if (dyConsumed > 0 && child.visibility == View.VISIBLE) {
            child.hide(object : FloatingActionButton.OnVisibilityChangedListener() {
                override fun onHidden(fab: FloatingActionButton?) {
                    super.onHidden(fab)
                    fab!!.visibility = View.INVISIBLE
                }
            })
        } else if (dyConsumed < 0 && child.visibility != View.VISIBLE) {
            child.show()
        }
    }
}