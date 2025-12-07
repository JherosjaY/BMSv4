package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.utils.StatusColorUtil;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BlotterReportAdapter extends RecyclerView.Adapter<BlotterReportAdapter.ReportViewHolder> {
    
    private List<BlotterReport> reports;
    private OnReportClickListener listener;
    
    public interface OnReportClickListener {
        void onReportClick(BlotterReport report);
    }
    
    public BlotterReportAdapter(List<BlotterReport> reports, OnReportClickListener listener) {
        this.reports = reports;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        BlotterReport report = reports.get(position);
        holder.bind(report, listener);
    }
    
    @Override
    public int getItemCount() {
        return reports.size();
    }
    
    public void updateReports(List<BlotterReport> newReports) {
        this.reports = newReports;
        notifyDataSetChanged();
    }
    
    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private TextView tvCaseNumber, tvIncidentType, tvDate, tvLocation;
        private TextView tvComplainantName, tvAssignedOfficers;
        private Chip chipStatus;
        
        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardReport);
            tvCaseNumber = itemView.findViewById(R.id.tvCaseNumber);
            tvIncidentType = itemView.findViewById(R.id.tvIncidentType);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvComplainantName = itemView.findViewById(R.id.tvComplainantName);
            tvAssignedOfficers = itemView.findViewById(R.id.tvAssignedOfficers);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
        
        public void bind(BlotterReport report, OnReportClickListener listener) {
            tvCaseNumber.setText(report.getCaseNumber());
            tvIncidentType.setText(report.getIncidentType());
            tvLocation.setText(report.getLocation());
            tvComplainantName.setText(report.getComplainantName() != null ? report.getComplainantName() : "Unknown");
            
            // Format date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            tvDate.setText(dateFormat.format(new Date(report.getIncidentDate())));
            
            // ✅ Set status chip using global utility
            if (chipStatus != null) {
                String status = report.getStatus() != null ? report.getStatus() : "Pending";
                String displayStatus = StatusColorUtil.formatStatusToTitleCase(status);
                chipStatus.setText(displayStatus);
                chipStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.white));
                int statusColor = StatusColorUtil.getStatusColor(status);
                chipStatus.setChipBackgroundColorResource(statusColor);
            }
            
            // Set assigned officers - count the number of officers
            int officerCount = 0;
            
            // Check if there are multiple officers assigned (comma-separated IDs)
            if (report.getAssignedOfficerIds() != null && !report.getAssignedOfficerIds().isEmpty()) {
                String[] officerIds = report.getAssignedOfficerIds().split(",");
                officerCount = officerIds.length;
                tvAssignedOfficers.setText(String.valueOf(officerCount));
            } 
            // Check if there's a single officer assigned
            else if (report.getAssignedOfficerId() != null && report.getAssignedOfficerId() > 0) {
                officerCount = 1;
                tvAssignedOfficers.setText("1");
            } 
            // No officers assigned
            else {
                tvAssignedOfficers.setText("0");
            }
            
            // Click listener
            if (cardView != null) {
                cardView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onReportClick(report);
                    }
                });
            }
        }
        
        // ✅ Using global StatusColorUtil for status formatting and colors
    }
}
