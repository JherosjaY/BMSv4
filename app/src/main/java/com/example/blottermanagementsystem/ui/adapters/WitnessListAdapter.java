package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.Witness;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WitnessListAdapter extends RecyclerView.Adapter<WitnessListAdapter.ViewHolder> {
    
    private List<Witness> witnessList;
    private boolean isReadOnly;
    
    public WitnessListAdapter(List<Witness> witnessList, boolean isReadOnly) {
        this.witnessList = witnessList;
        this.isReadOnly = isReadOnly;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_witness_card, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Witness witness = witnessList.get(position);
        
        holder.tvName.setText(witness.getName());
        holder.tvContactInfo.setText(witness.getContactNumber() != null && !witness.getContactNumber().isEmpty() ? witness.getContactNumber() : "N/A");
        holder.tvStatement.setText(witness.getStatement() != null ? witness.getStatement() : "");
        holder.tvRecordedBy.setText("Recorded by: Officer");
        
        // Format date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String dateStr = sdf.format(new Date(witness.getCreatedAt()));
        holder.tvDate.setText(dateStr);
    }
    
    @Override
    public int getItemCount() {
        return witnessList.size();
    }
    
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvContactInfo;
        TextView tvStatement;
        TextView tvRecordedBy;
        TextView tvDate;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvWitnessName);
            tvContactInfo = itemView.findViewById(R.id.tvWitnessContact);
            tvStatement = itemView.findViewById(R.id.tvWitnessStatement);
            tvRecordedBy = itemView.findViewById(R.id.tvRecordedBy);
            tvDate = itemView.findViewById(R.id.tvRecordedDate);
        }
    }
}
