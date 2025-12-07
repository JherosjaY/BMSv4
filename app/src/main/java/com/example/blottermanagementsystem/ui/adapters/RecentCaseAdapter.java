package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.utils.StatusColorUtil;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecentCaseAdapter extends RecyclerView.Adapter<RecentCaseAdapter.CaseViewHolder> {
    
    private List<BlotterReport> cases;
    private OnCaseClickListener listener;
    
    public interface OnCaseClickListener {
        void onCaseClick(BlotterReport report);
    }
    
    public RecentCaseAdapter(List<BlotterReport> cases, OnCaseClickListener listener) {
        this.cases = cases;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public CaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_recent_case, parent, false);
        return new CaseViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull CaseViewHolder holder, int position) {
        BlotterReport report = getItem(position);
        if (report != null) {
            holder.bind(report, listener);
        }
    }
    
    @Override
    public int getItemCount() {
        // Limit to max 3 recent cases
        return Math.min(cases.size(), 3);
    }
    
    public void updateCases(List<BlotterReport> newCases) {
        this.cases = newCases;
        notifyDataSetChanged();
    }
    
    private BlotterReport getItem(int position) {
        // ✅ FIXED: Show cases in correct sorted order (NOT reversed)
        // Cases are already sorted by priority in OfficerDashboardActivity
        if (position >= 0 && position < cases.size()) {
            return cases.get(position);
        }
        return null;
    }
    
    // ✅ Using global StatusColorUtil for status formatting
    
    static class CaseViewHolder extends RecyclerView.ViewHolder {
        private View itemView;
        private TextView tvCaseNumber, tvIncidentType, tvDate, tvComplainantName, tvLocation, tvAssignedOfficers;
        private com.google.android.material.chip.Chip chipStatus;
        
        public CaseViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvCaseNumber = itemView.findViewById(R.id.tvCaseNumber);
            tvIncidentType = itemView.findViewById(R.id.tvIncidentType);
            tvDate = itemView.findViewById(R.id.tvDate);
            chipStatus = itemView.findViewById(R.id.tvStatus);
            tvComplainantName = itemView.findViewById(R.id.tvComplainantName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvAssignedOfficers = itemView.findViewById(R.id.tvAssignedOfficers);
        }
        
        public void bind(BlotterReport report, OnCaseClickListener listener) {
            tvCaseNumber.setText(report.getCaseNumber());
            tvIncidentType.setText(report.getIncidentType());
            // ✅ Format status using global utility
            String status = report.getStatus();
            String displayStatus = StatusColorUtil.formatStatusToTitleCase(status);
            chipStatus.setText(displayStatus);
            chipStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.white));
            
            // ✅ Set chip background color using global utility
            int statusColor = StatusColorUtil.getStatusColor(status);
            chipStatus.setChipBackgroundColorResource(statusColor);
            
            // Format date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDate.setText(dateFormat.format(new Date(report.getIncidentDate())));
            
            // Set complainant name
            tvComplainantName.setText(report.getComplainantName() != null ? report.getComplainantName() : "Unknown");
            
            // Set location
            tvLocation.setText(report.getLocation() != null ? report.getLocation() : "Not specified");
            
            // Set assigned officers
            String officers = report.getAssignedOfficerIds() != null && !report.getAssignedOfficerIds().isEmpty() 
                ? report.getAssignedOfficerIds() 
                : "Unassigned";
            tvAssignedOfficers.setText(officers);
            
            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCaseClick(report);
                }
            });
        }
    }
}
