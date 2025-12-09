package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import java.util.List;

public class RecentCasesAdapter extends RecyclerView.Adapter<RecentCasesAdapter.ViewHolder> {
    
    private List<BlotterReport> cases;
    
    public RecentCasesAdapter(List<BlotterReport> cases) {
        this.cases = cases;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BlotterReport report = cases.get(position);
        
        if (holder.tvCaseNumber != null) {
            holder.tvCaseNumber.setText("Case: " + (report.getCaseNumber() != null ? report.getCaseNumber() : "N/A"));
        }
        if (holder.tvIncidentType != null) {
            holder.tvIncidentType.setText(report.getIncidentType() != null ? report.getIncidentType() : "Unknown");
        }
        if (holder.tvStatus != null) {
            holder.tvStatus.setText("Status: " + (report.getStatus() != null ? report.getStatus() : "Pending"));
        }
    }
    
    @Override
    public int getItemCount() {
        return cases != null ? cases.size() : 0;
    }
    
    public void updateCases(List<BlotterReport> newCases) {
        this.cases = newCases;
        notifyDataSetChanged();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView tvCaseNumber;
        public TextView tvIncidentType;
        public TextView tvStatus;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCaseNumber = itemView.findViewById(R.id.tvCaseNumber);
            tvIncidentType = itemView.findViewById(R.id.tvIncidentType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
