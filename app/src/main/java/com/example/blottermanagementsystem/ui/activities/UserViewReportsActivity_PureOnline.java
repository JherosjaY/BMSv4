// PURE ONLINE REFACTORING - Replace loadReports() method with this:

private void loadReports() {
    android.util.Log.d("UserViewReports", "=== PURE ONLINE: Loading user reports from API ===");
    
    String userId = preferencesManager.getUserId();
    
    // ✅ PURE ONLINE: Check internet first
    com.example.blottermanagementsystem.utils.NetworkMonitor networkMonitor = 
        new com.example.blottermanagementsystem.utils.NetworkMonitor(this);
    
    if (!networkMonitor.isNetworkAvailable()) {
        android.util.Log.e("UserViewReports", "❌ No internet connection");
        runOnUiThread(() -> {
            android.widget.Toast.makeText(this, "No internet connection", android.widget.Toast.LENGTH_SHORT).show();
            if (recyclerView != null) recyclerView.setVisibility(android.view.View.GONE);
            if (emptyState != null) emptyState.setVisibility(android.view.View.VISIBLE);
        });
        return;
    }
    
    // Online - load from API
    android.util.Log.d("UserViewReports", "🌐 Loading reports from API");
    com.example.blottermanagementsystem.utils.ApiClient.getAllReports(
        new com.example.blottermanagementsystem.utils.ApiClient.ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport>>() {
            @Override
            public void onSuccess(java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport> apiReports) {
                android.util.Log.d("UserViewReports", "✅ Loaded " + apiReports.size() + " reports from API");
                
                // Filter reports by current user
                java.util.List<com.example.blottermanagementsystem.data.entity.BlotterReport> userReports = new java.util.ArrayList<>();
                for (com.example.blottermanagementsystem.data.entity.BlotterReport report : apiReports) {
                    if (report.getReportedById().equals(userId)) {
                        userReports.add(report);
                    }
                }
                
                runOnUiThread(() -> {
                    allReports.clear();
                    allReports.addAll(userReports);
                    updateStatistics();
                    filterReports();
                    
                    if (userReports.isEmpty()) {
                        if (recyclerView != null) recyclerView.setVisibility(android.view.View.GONE);
                        if (emptyState != null) emptyState.setVisibility(android.view.View.VISIBLE);
                    } else {
                        if (recyclerView != null) recyclerView.setVisibility(android.view.View.VISIBLE);
                        if (emptyState != null) emptyState.setVisibility(android.view.View.GONE);
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.e("UserViewReports", "❌ Failed to load reports: " + errorMessage);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(UserViewReportsActivity.this, "Failed to load reports", android.widget.Toast.LENGTH_SHORT).show();
                    if (recyclerView != null) recyclerView.setVisibility(android.view.View.GONE);
                    if (emptyState != null) emptyState.setVisibility(android.view.View.VISIBLE);
                });
            }
        });
}
