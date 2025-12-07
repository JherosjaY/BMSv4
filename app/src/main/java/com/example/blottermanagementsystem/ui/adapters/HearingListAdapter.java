package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.Hearing;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HearingListAdapter extends RecyclerView.Adapter<HearingListAdapter.ViewHolder> {
    
    private List<Hearing> hearingList;
    private boolean isReadOnly;
    private OnEditHearingListener editListener;
    
    public interface OnEditHearingListener {
        void onEditHearing(Hearing hearing);
    }
    
    public HearingListAdapter(List<Hearing> hearingList, boolean isReadOnly) {
        this.hearingList = hearingList;
        this.isReadOnly = isReadOnly;
    }
    
    public void setEditListener(OnEditHearingListener listener) {
        this.editListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hearing_card, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Hearing hearing = hearingList.get(position);
        
        holder.tvType.setText(hearing.getPurpose() != null ? hearing.getPurpose() : "Hearing");
        holder.tvLocation.setText("📍 " + hearing.getLocation());
        holder.tvNotes.setText(hearing.getStatus() != null ? hearing.getStatus() : "");
        holder.tvScheduledBy.setText("Status: " + (hearing.getStatus() != null ? hearing.getStatus() : "Scheduled"));
        
        // Format hearing date and time
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String hearingDateStr = hearing.getHearingDate() != null ? hearing.getHearingDate() : "";
        String hearingTimeStr = hearing.getHearingTime() != null ? hearing.getHearingTime() : "";
        holder.tvHearingDate.setText("📅 " + hearingDateStr + " " + hearingTimeStr);
        
        // Format created date
        String createdDateStr = sdf.format(new Date(hearing.getCreatedAt()));
        holder.tvScheduledDate.setText(createdDateStr);
    }
    
    @Override
    public int getItemCount() {
        return hearingList.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType;
        TextView tvLocation;
        TextView tvNotes;
        TextView tvHearingDate;
        TextView tvScheduledBy;
        TextView tvScheduledDate;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvHearingType);
            tvLocation = itemView.findViewById(R.id.tvHearingLocation);
            tvNotes = itemView.findViewById(R.id.tvHearingNotes);
            tvHearingDate = itemView.findViewById(R.id.tvHearingDate);
            tvScheduledBy = itemView.findViewById(R.id.tvScheduledBy);
            tvScheduledDate = itemView.findViewById(R.id.tvScheduledDate);
        }
    }
}
