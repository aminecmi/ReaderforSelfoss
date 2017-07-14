package apps.amine.bou.readerforselfoss.utils

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.customtabs.CustomTabsIntent

import xyz.klinker.android.drag_dismiss.DragDismissIntentBuilder

import apps.amine.bou.readerforselfoss.R
import apps.amine.bou.readerforselfoss.ReaderActivity
import apps.amine.bou.readerforselfoss.api.selfoss.Item
import apps.amine.bou.readerforselfoss.utils.customtabs.CustomTabActivityHelper



fun Context.buildCustomTabsIntent(): CustomTabsIntent {

    val actionIntent = Intent(Intent.ACTION_SEND)
    actionIntent.type = "text/plain"
    val createPendingShareIntent: PendingIntent = PendingIntent.getActivity(this, 0, actionIntent, 0)


    val intentBuilder = CustomTabsIntent.Builder()

    // TODO: change to primary when it's possible to customize custom tabs title color
    //intentBuilder.setToolbarColor(c.getResources().getColor(R.color.colorPrimary));
    intentBuilder.setToolbarColor(resources.getColor(R.color.colorAccentDark))
    intentBuilder.setShowTitle(true)


    intentBuilder.setStartAnimations(this,
            R.anim.slide_in_right,
            R.anim.slide_out_left)
    intentBuilder.setExitAnimations(this,
            android.R.anim.slide_in_left,
            android.R.anim.slide_out_right)

    val closeicon = BitmapFactory.decodeResource(resources, R.drawable.ic_close_white_24dp)
    intentBuilder.setCloseButtonIcon(closeicon)

    val shareLabel = this.getString(R.string.label_share)
    val icon = BitmapFactory.decodeResource(resources,
            R.drawable.ic_share_white_24dp)
    intentBuilder.setActionButton(icon, shareLabel, createPendingShareIntent)

    return intentBuilder.build()
}

fun Context.openItemUrl(linkDecoded: String,
                        customTabsIntent: CustomTabsIntent,
                        internalBrowser: Boolean,
                        articleViewer: Boolean,
                        app: Activity) {
    if (!internalBrowser) {
        openInBrowser(linkDecoded, app)
    } else {
        if (articleViewer) {
            val intent = Intent(this, ReaderActivity::class.java)

            DragDismissIntentBuilder(this)
                    .setFullscreenOnTablets(true)      // defaults to false, tablets will have padding on each side
                    .setDragElasticity(DragDismissIntentBuilder.DragElasticity.NORMAL)  // Larger elasticities will make it easier to dismiss.
                    .build(intent)

            intent.putExtra("url", linkDecoded)
            app.startActivity(intent)
        } else {
            try {
                CustomTabActivityHelper.openCustomTab(app, customTabsIntent, Uri.parse(linkDecoded)
                ) { _, uri ->
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            } catch (e: Exception) {
                openInBrowser(linkDecoded, app)
            }
        }
    }
}

private fun openInBrowser(linkDecoded: String, app: Activity) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(linkDecoded)
    app.startActivity(intent)
}
