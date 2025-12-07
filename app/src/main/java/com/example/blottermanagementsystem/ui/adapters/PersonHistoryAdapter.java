package com.example.blottermanagementsystem.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.PersonHistory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PersonHistoryAdapter extends RecyclerView.Adapter<PersonHistoryAdapter.ViewHolder> {
    
    private List<PersonHistory> historyList;
    private Context context;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private OnHistoryClickListener clickListener;
    
    // ✅ Click listener interface
    public interface OnHistoryClickListener {
        void onHistoryClick(PersonHistory history);
    }
    
    public PersonHistoryAdapter(List<PersonHistory> historyList, Context context) {
        this.historyList = historyList;
        this.context = context;
    }
    
    // ✅ Set click listener
    public void setOnHistoryClickListener(OnHistoryClickListener listener) {
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_person_history, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PersonHistory history = historyList.get(position);
        
        // ✅ Case Number
        if (holder.tvCaseNumber != null) {
            holder.tvCaseNumber.setText("Case #" + history.getBlotterReportId());
        }
        
        // ✅ Activity Type (Suspect/Respondent)
        if (holder.tvActivityType != null) {
            String activityType = history.getActivityType() != null ? 
                history.getActivityType() : "Unknown";
            holder.tvActivityType.setText(activityType);
        }
        
        // ✅ Description
        if (holder.tvDescription != null) {
            String description = history.getDescription() != null ? 
                history.getDescription() : "No description";
            holder.tvDescription.setText(description);
        }
        
        // ✅ Timestamp
        if (holder.tvTimestamp != null) {
            String timestamp = formatTimestamp(history.getTimestamp());
            holder.tvTimestamp.setText(timestamp);
        }
        
        // ✅ Metadata (Resolution Type)
        if (holder.tvMetadata != null && history.getMetadata() != null) {
            String metadata = history.getMetadata();
            if (metadata.contains("resolution_type:")) {
                String resolutionType = metadata.split("resolution_type:")[1].split(",")[0];
                holder.tvMetadata.setText("Resolution: " + resolutionType);
                holder.tvMetadata.setVisibility(View.VISIBLE);
            } else {
                holder.tvMetadata.setVisibility(View.GONE);
            }
        }
        
        // ✅ CLICK LISTENER - Open full case details
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onHistoryClick(history);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return historyList.size();
    }
    
    // ✅ FORMAT TIMESTAMP
    private String formatTimestamp(long timestamp) {
        try {
            Date date = new Date(timestamp);
            return dateFormat.format(date) + " " + timeFormat.format(date);
        } catch (Exception e) {
            return "Unknown date";
        }
    }
    
    // ✅ UPDATE DATA
    public void updateData(List<PersonHistory> newList) {
        this.historyList = newList;
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCaseNumber, tvActivityType, tvDescription, tvTimestamp, tvMetadata;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvCaseNumber = itemView.findViewById(R.id.tvCaseNumber);
            tvActivityType = itemView.findViewById(R.id.tvActivityType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvMetadata = itemView.findViewById(R.id.tvMetadata);
        }
    }
}
