package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.Suspect;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SuspectListAdapter extends RecyclerView.Adapter<SuspectListAdapter.ViewHolder> {
    
    private List<Suspect> suspectList;
    private boolean isReadOnly;
    
    public SuspectListAdapter(List<Suspect> suspectList, boolean isReadOnly) {
        this.suspectList = suspectList;
        this.isReadOnly = isReadOnly;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_suspect_card, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Suspect suspect = suspectList.get(position);
        
        // Format name with alias: "Full Name/Alias" or just "Full Name" if no alias
        String displayName = suspect.getName();
        if (suspect.getAlias() != null && !suspect.getAlias().isEmpty()) {
            displayName = suspect.getName() + "/" + suspect.getAlias();
        }
        holder.tvName.setText(displayName);
        
        holder.tvContactInfo.setText(suspect.getAddress() != null && !suspect.getAddress().isEmpty() ? suspect.getAddress() : "N/A");
        holder.tvDescription.setText(suspect.getDescription() != null ? suspect.getDescription() : "");
        holder.tvIdentifiedBy.setText("Identified by: Officer");
        
        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(suspect.getDateAdded()));
        holder.tvDate.setText(dateStr);
    }
    
    @Override
    public int getItemCount() {
        return suspectList.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvContactInfo;
        TextView tvDescription;
        TextView tvIdentifiedBy;
        TextView tvDate;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSuspectName);
            tvContactInfo = itemView.findViewById(R.id.tvSuspectContact);
            tvDescription = itemView.findViewById(R.id.tvSuspectDescription);
            tvIdentifiedBy = itemView.findViewById(R.id.tvIdentifiedBy);
            tvDate = itemView.findViewById(R.id.tvIdentifiedDate);
        }
    }
}
