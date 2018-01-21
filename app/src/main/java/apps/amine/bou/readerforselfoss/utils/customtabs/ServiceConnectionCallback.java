package apps.amine.bou.readerforselfoss.utils.customtabs;


import android.support.customtabs.CustomTabsClient;


public interface ServiceConnectionCallback {
    /**
     * Called when the service is connected.
     *
     * @param client a CustomTabsClient
     */
    void onServiceConnected(CustomTabsClient client);

    /**
     * Called when the service is disconnected.
     */
    void onServiceDisconnected();
}
