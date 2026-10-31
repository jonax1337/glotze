package local.tvhome;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.ImageView;

/** Shows whether the device is online, and over which link, as a small status glyph. */
final class NetworkIndicator extends ConnectivityManager.NetworkCallback {
    interface Listener {
        void onOnlineChanged(boolean online);
    }

    private final ImageView status;
    private final Listener listener;
    private final ConnectivityManager connectivity;

    NetworkIndicator(ImageView status, Listener listener) {
        this.status = status;
        this.listener = listener;
        this.connectivity = status.getContext().getSystemService(ConnectivityManager.class);
    }

    void start() {
        showOffline();
        connectivity.registerDefaultNetworkCallback(this);
    }

    void stop() {
        connectivity.unregisterNetworkCallback(this);
    }

    /** Android 7 reports an already-connected network only through this callback. */
    @Override
    public void onAvailable(Network network) {
        showLink(connectivity.getNetworkCapabilities(network));
    }

    @Override
    public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
        showLink(capabilities);
    }

    @Override
    public void onLost(Network network) {
        showOffline();
    }

    private void showLink(NetworkCapabilities capabilities) {
        boolean wired = capabilities != null
                && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        if (wired) {
            showOnline(R.attr.iconEthernet, R.string.network_ethernet);
        } else {
            showOnline(R.attr.iconNetwork, R.string.network_wifi);
        }
    }

    /** Callbacks arrive on a binder thread; views may only be touched on the UI thread. */
    private void showOnline(int iconAttr, int descriptionRes) {
        status.post(() -> {
            show(iconAttr, descriptionRes);
            listener.onOnlineChanged(true);
        });
    }

    private void showOffline() {
        status.post(() -> {
            show(R.attr.iconWifiOff, R.string.network_offline);
            listener.onOnlineChanged(false);
        });
    }

    private void show(int iconAttr, int descriptionRes) {
        status.setImageResource(ThemeValues.resource(status.getContext(), iconAttr));
        status.setContentDescription(status.getContext().getString(descriptionRes));
    }
}
