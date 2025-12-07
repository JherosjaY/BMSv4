package com.example.blottermanagementsystem.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.database.BlotterDatabase;
import com.example.blottermanagementsystem.data.entity.BlotterReport;
import com.example.blottermanagementsystem.data.entity.Hearing;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HearingAdapter extends RecyclerView.Adapter<HearingAdapter.ViewHolder> {
    
    private List<Hearing> hearings = new ArrayList<>();
    private OnHearingClickListener listener;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    
    public interface OnHearingClickListener {
        void onHearingClick(Hearing hearing);
    }
    
    public HearingAdapter(Context context, OnHearingClickListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    // ✅ Constructor with initial data
    public HearingAdapter(Context context, List<Hearing> hearings, OnHearingClickListener listener) {
        this.context = context;
        this.hearings = hearings != null ? hearings : new ArrayList<>();
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_hearing, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Hearing hearing = hearings.get(position);
        
        // ✅ Fetch case number from database
        if (holder.tvCaseNumber != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    BlotterDatabase database = BlotterDatabase.getDatabase(context);
                    BlotterReport report = database.blotterReportDao().getReportById(hearing.getBlotterReportId());
                    String caseNumber = (report != null && report.getCaseNumber() != null) 
                        ? "Case #" + report.getCaseNumber() 
                        : "Case #TBD";
                    holder.tvCaseNumber.post(() -> holder.tvCaseNumber.setText(caseNumber));
                } catch (Exception e) {
                    holder.tvCaseNumber.post(() -> holder.tvCaseNumber.setText("Case #TBD"));
                }
            });
        }
        if (holder.tvPurpose != null) {
            holder.tvPurpose.setText(hearing.getPurpose() != null ? hearing.getPurpose() : "");
        }
        if (holder.tvDate != null) {
            // ✅ Handle hearing date and time - stored as strings
            String dateTimeDisplay = "";
            if (hearing.getHearingDate() != null && !hearing.getHearingDate().isEmpty()) {
                dateTimeDisplay = hearing.getHearingDate();
                if (hearing.getHearingTime() != null && !hearing.getHearingTime().isEmpty()) {
                    dateTimeDisplay += " at " + hearing.getHearingTime();
                }
            }
            holder.tvDate.setText(dateTimeDisplay.isEmpty() ? "TBD" : dateTimeDisplay);
        }
        if (holder.tvLocation != null) {
            holder.tvLocation.setText(hearing.getLocation() != null ? hearing.getLocation() : "");
        }
        
        // ✅ Display on card (visible)
        if (holder.tvCardDate != null) {
            holder.tvCardDate.setText(hearing.getHearingDate() != null ? hearing.getHearingDate() : "TBD");
        }
        if (holder.tvCardTime != null) {
            holder.tvCardTime.setText(hearing.getHearingTime() != null ? hearing.getHearingTime() : "TBD");
        }
        
        // ✅ Presiding officer not available in Hearing entity, set placeholder
        if (holder.tvPresidingOfficer != null) {
            holder.tvPresidingOfficer.setText("Officer");
        }
        // ✅ Update hearing status based on case status
        if (holder.chipStatus != null) {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    BlotterDatabase database = BlotterDatabase.getDatabase(context);
                    BlotterReport report = database.blotterReportDao().getReportById(hearing.getBlotterReportId());
                    
                    String displayStatus = "Scheduled";
                    if (report != null && report.getStatus() != null) {
                        String caseStatus = report.getStatus();
                        
                        // Map case status to hearing status
                        if ("Resolved".equalsIgnoreCase(caseStatus)) {
                            displayStatus = "Completed";  // Case resolved = Hearing completed
                        } else if ("Cancelled".equalsIgnoreCase(caseStatus)) {
                            displayStatus = "Cancelled";  // Case cancelled = Hearing cancelled
                        } else {
                            displayStatus = "Scheduled";  // Default
                        }
                    }
                    
                    String finalStatus = displayStatus;
                    holder.chipStatus.post(() -> holder.chipStatus.setText(finalStatus));
                } catch (Exception e) {
                    holder.chipStatus.post(() -> holder.chipStatus.setText("Scheduled"));
                }
            });
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onHearingClick(hearing);
        });
    }
    
    @Override
    public int getItemCount() {
        return hearings.size();
    }
    
    public void setHearings(List<Hearing> hearings) {
        this.hearings = hearings != null ? hearings : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    // ✅ Update data reference (for filtering)
    public void updateData(List<Hearing> newHearings) {
        this.hearings = newHearings != null ? newHearings : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCaseNumber, tvPurpose, tvDate, tvLocation, tvPresidingOfficer, tvCardDate, tvCardTime;
        com.google.android.material.chip.Chip chipStatus;
        
        ViewHolder(View itemView) {
            super(itemView);
            // ✅ Map to correct view IDs from item_hearing.xml
            tvCaseNumber = itemView.findViewById(R.id.tvCaseNumber);
            tvPurpose = itemView.findViewById(R.id.tvPurpose);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPresidingOfficer = itemView.findViewById(R.id.tvPresidingOfficer);
            tvCardDate = itemView.findViewById(R.id.tvCardDate);
            tvCardTime = itemView.findViewById(R.id.tvCardTime);
            chipStatus = itemView.findViewById(R.id.chipStatus);
        }
    }
}
