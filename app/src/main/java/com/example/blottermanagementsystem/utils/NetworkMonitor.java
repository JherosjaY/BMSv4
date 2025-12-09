package com.example.blottermanagementsystem.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import androidx.annotation.NonNull;

public class NetworkMonitor {
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private NetworkCallback networkCallback;
    
    public interface NetworkCallback {
        void onNetworkAvailable();
        void onNetworkLost();
    }
    
    public NetworkMonitor(Context context) {
        this.context = context;
        this.connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    public boolean isNetworkAvailable() {
        if (connectivityManager == null) return false;
        
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;
        
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }
    
    // Alias for compatibility
    public boolean isOnline() {
        return isNetworkAvailable();
    }
    
    public void startMonitoring(NetworkCallback callback) {
        this.networkCallback = callback;
        
        NetworkRequest request = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();
        
        connectivityManager.registerNetworkCallback(request, 
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    if (networkCallback != null) {
                        networkCallback.onNetworkAvailable();
                    }
                }
                
                @Override
                public void onLost(@NonNull Network network) {
                    if (networkCallback != null) {
                        networkCallback.onNetworkLost();
                    }
                }
            });
    }
}
