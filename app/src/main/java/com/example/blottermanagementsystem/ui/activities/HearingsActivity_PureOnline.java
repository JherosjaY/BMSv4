// PURE ONLINE REFACTORING - Replace loadHearings() method with this:

private void loadHearings() {
    android.util.Log.d("HearingsActivity", "=== PURE ONLINE: Loading hearings from API ===");
    
    String userId = preferencesManager.getUserId();
    String userRole = roleManager.getUserRole();
    
    // ✅ PURE ONLINE: Check internet first
    com.example.blottermanagementsystem.utils.NetworkMonitor networkMonitor = 
        new com.example.blottermanagementsystem.utils.NetworkMonitor(this);
    
    if (!networkMonitor.isNetworkAvailable()) {
        android.util.Log.e("HearingsActivity", "❌ No internet connection");
        runOnUiThread(() -> {
            android.widget.Toast.makeText(this, "No internet connection", android.widget.Toast.LENGTH_SHORT).show();
            if (recyclerView != null) recyclerView.setVisibility(android.view.View.GONE);
            if (emptyStateCard != null) emptyStateCard.setVisibility(android.view.View.VISIBLE);
        });
        return;
    }
    
    // Online - load from API
    android.util.Log.d("HearingsActivity", "🌐 Loading hearings from API for role: " + userRole);
    com.example.blottermanagementsystem.utils.ApiClient.getHearings(userId,
        new com.example.blottermanagementsystem.utils.ApiClient.ApiCallback<java.util.List<com.example.blottermanagementsystem.data.entity.Hearing>>() {
            @Override
            public void onSuccess(java.util.List<com.example.blottermanagementsystem.data.entity.Hearing> apiHearings) {
                android.util.Log.d("HearingsActivity", "✅ Loaded " + apiHearings.size() + " hearings from API");
                
                // Filter hearings based on user role
                java.util.List<com.example.blottermanagementsystem.data.entity.Hearing> userHearings = new java.util.ArrayList<>();
                
                if ("OFFICER".equalsIgnoreCase(userRole)) {
                    // Officers see hearings for their assigned cases
                    int officerId = roleManager.getOfficerId();
                    for (com.example.blottermanagementsystem.data.entity.Hearing hearing : apiHearings) {
                        if (hearing.getOfficerId() == officerId) {
                            userHearings.add(hearing);
                        }
                    }
                } else if ("ADMIN".equalsIgnoreCase(userRole)) {
                    // Admins see all hearings
                    userHearings.addAll(apiHearings);
                } else {
                    // Users see hearings for their own reports
                    for (com.example.blottermanagementsystem.data.entity.Hearing hearing : apiHearings) {
                        if (hearing.getUserId().equals(userId)) {
                            userHearings.add(hearing);
                        }
                    }
                }
                
                runOnUiThread(() -> {
                    allHearings.clear();
                    allHearings.addAll(userHearings);
                    filterHearings();
                    
                    if (userHearings.isEmpty()) {
                        if (recyclerView != null) recyclerView.setVisibility(android.view.View.GONE);
                        if (emptyStateCard != null) emptyStateCard.setVisibility(android.view.View.VISIBLE);
                    } else {
                        if (recyclerView != null) recyclerView.setVisibility(android.view.View.VISIBLE);
                        if (emptyStateCard != null) emptyStateCard.setVisibility(android.view.View.GONE);
                    }
                });
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.e("HearingsActivity", "❌ Failed to load hearings: " + errorMessage);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(HearingsActivity.this, "Failed to load hearings", android.widget.Toast.LENGTH_SHORT).show();
                    if (recyclerView != null) recyclerView.setVisibility(android.view.View.GONE);
                    if (emptyStateCard != null) emptyStateCard.setVisibility(android.view.View.VISIBLE);
                });
            }
        });
}
