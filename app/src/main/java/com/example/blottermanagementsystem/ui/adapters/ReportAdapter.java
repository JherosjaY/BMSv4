package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.utils.StatusColorUtil;
import java.text.SimpleDateFormat;
import com.google.android.material.chip.Chip;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    
    private List<BlotterReport> reports;
    private OnReportClickListener listener;
    
    public interface OnReportClickListener {
        void onReportClick(BlotterReport report);
    }
    
    public ReportAdapter(List<BlotterReport> reports, OnReportClickListener listener) {
        this.reports = reports;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        android.util.Log.d("ReportAdapter", "onCreateViewHolder() called");
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_report, parent, false);
        android.util.Log.d("ReportAdapter", "Item view inflated successfully");
        return new ReportViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        BlotterReport report = reports.get(position);
        android.util.Log.d("ReportAdapter", "Binding report at position " + position + ": " + report.getCaseNumber());
        holder.bind(report, listener);
    }
    
    @Override
    public int getItemCount() {
        int count = reports.size();
        android.util.Log.d("ReportAdapter", "getItemCount() returning " + count);
        return count;
    }
    
    public void updateReports(List<BlotterReport> newReports) {
        this.reports = newReports;
        notifyDataSetChanged();
    }
    
    public void setReports(List<BlotterReport> newReports) {
        updateReports(newReports);
    }
    
    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCaseNumber, tvIncidentType, tvComplainantName, tvLocation, tvDate;
        private TextView tvImageCount, tvVideoCount, tvAssignedOfficers;
        private View layoutEvidence, cardImageBadge, cardVideoBadge;
        private Chip chipStatus;
        
        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCaseNumber = itemView.findViewById(R.id.tvCaseNumber);
            tvIncidentType = itemView.findViewById(R.id.tvIncidentType);
            tvComplainantName = itemView.findViewById(R.id.tvComplainantName);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDate = itemView.findViewById(R.id.tvDate);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            tvImageCount = itemView.findViewById(R.id.tvImageCount);
            tvVideoCount = itemView.findViewById(R.id.tvVideoCount);
            tvAssignedOfficers = itemView.findViewById(R.id.tvAssignedOfficers);
            layoutEvidence = itemView.findViewById(R.id.layoutEvidence);
            cardImageBadge = itemView.findViewById(R.id.cardImageBadge);
            cardVideoBadge = itemView.findViewById(R.id.cardVideoBadge);
        }
        
        public void bind(BlotterReport report, OnReportClickListener listener) {
            tvCaseNumber.setText(report.getCaseNumber());
            tvIncidentType.setText(report.getIncidentType());
            tvComplainantName.setText(report.getComplainantName());
            tvLocation.setText(report.getIncidentLocation());
            tvDate.setText(new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(new java.util.Date(report.getIncidentDate())));
            
            // Set assigned officers
            String assignedOfficer = report.getAssignedOfficer();
            if (assignedOfficer != null && !assignedOfficer.isEmpty()) {
                tvAssignedOfficers.setText(assignedOfficer);
            } else {
                tvAssignedOfficers.setText("Unassigned");
            }
            
            // ✅ Set status chip using global utility
            String status = report.getStatus();
            String displayStatus = StatusColorUtil.formatStatusToTitleCase(status);
            chipStatus.setText(displayStatus);
            int statusColor = StatusColorUtil.getStatusColor(status);
            chipStatus.setChipBackgroundColorResource(statusColor);
            
            // Count evidence
            int imageCount = countItems(report.getImageUris());
            int videoCount = countItems(report.getVideoUris());
            
            // Show/hide evidence indicators
            boolean hasEvidence = imageCount > 0 || videoCount > 0;
            layoutEvidence.setVisibility(hasEvidence ? View.VISIBLE : View.GONE);
            
            if (imageCount > 0) {
                cardImageBadge.setVisibility(View.VISIBLE);
                tvImageCount.setText(String.valueOf(imageCount));
            } else {
                cardImageBadge.setVisibility(View.GONE);
            }
            
            if (videoCount > 0) {
                cardVideoBadge.setVisibility(View.VISIBLE);
                tvVideoCount.setText(String.valueOf(videoCount));
            } else {
                cardVideoBadge.setVisibility(View.GONE);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReportClick(report);
                }
            });
        }
        
        private int countItems(String uris) {
            if (uris == null || uris.trim().isEmpty()) {
                return 0;
            }
            return uris.split(",").length;
        }
        
        // ✅ Using global StatusColorUtil for status formatting and colors
    }
}
