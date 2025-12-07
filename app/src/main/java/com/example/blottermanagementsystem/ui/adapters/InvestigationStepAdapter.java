package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.model.InvestigationStep;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class InvestigationStepAdapter extends RecyclerView.Adapter<InvestigationStepAdapter.StepViewHolder> {
    
    private List<InvestigationStep> steps;
    private OnStepActionListener listener;
    private boolean isInvestigationStarted = false;
    private String userRole = "OFFICER";  // Default to OFFICER, can be OFFICER, ADMIN, or USER
    private int reportId = -1;  // For view dialogs
    
    public interface OnStepActionListener {
        void onStepAction(InvestigationStep step);
        void onViewWitnesses(int reportId);
        void onViewSuspects(int reportId);
        void onViewEvidence(int reportId);
        void onViewHearings(int reportId);
        void onViewResolution(int reportId);
    }
    
    public InvestigationStepAdapter(List<InvestigationStep> steps, OnStepActionListener listener) {
        this.steps = steps;
        this.listener = listener;
    }
    
    public InvestigationStepAdapter(List<InvestigationStep> steps, OnStepActionListener listener, boolean isInvestigationStarted) {
        this.steps = steps;
        this.listener = listener;
        this.isInvestigationStarted = isInvestigationStarted;
    }
    
    public void setUserRole(String role) {
        this.userRole = role != null ? role : "OFFICER";
    }
    
    public void setReportId(int reportId) {
        this.reportId = reportId;
    }
    
    public void setInvestigationStarted(boolean started) {
        this.isInvestigationStarted = started;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public StepViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_investigation_step, parent, false);
        return new StepViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StepViewHolder holder, int position) {
        InvestigationStep step = steps.get(position);
        
        // Set step title and description
        holder.tvStepTitle.setText(step.getTitle());
        holder.tvStepDescription.setText(step.getDescription());
        
        // Update status indicator with icons (checkmarks, hourglasses, circles)
        if (step.isCompleted()) {
            // ✓ Completed - Green checkmark
            holder.ivStatus.setImageResource(R.drawable.ic_check_circle);
            holder.ivStatus.setColorFilter(holder.itemView.getContext()
                    .getColor(R.color.success_green));
        } else if (step.isInProgress()) {
            // ⏳ In Progress - Yellow hourglass icon
            holder.ivStatus.setImageResource(R.drawable.ic_hourglass);
            holder.ivStatus.setColorFilter(holder.itemView.getContext()
                    .getColor(R.color.warning_yellow));
        } else {
            // ⭕ Pending - Gray outline circle
            holder.ivStatus.setImageResource(R.drawable.ic_radio_button_unchecked);
            holder.ivStatus.setColorFilter(holder.itemView.getContext()
                    .getColor(R.color.text_secondary));
        }
        
        // Show/hide action button based on role
        if (step.getActionText() != null) {
            holder.btnAction.setVisibility(View.VISIBLE);
            
            if ("USER".equalsIgnoreCase(userRole)) {
                // USER ROLE: Show view buttons (always enabled, read-only)
                String buttonText = getViewButtonText(step.getTag());
                holder.btnAction.setText(buttonText);
                holder.btnAction.setIconResource(R.drawable.ic_visibility);  // Eye icon for view
                holder.btnAction.setEnabled(true);
                holder.btnAction.setAlpha(1.0f);
                
                holder.btnAction.setOnClickListener(v -> {
                    if (listener != null && reportId != -1) {
                        handleViewAction(step.getTag(), listener);
                    }
                });
            } else if ("OFFICER".equalsIgnoreCase(userRole)) {
                // OFFICER ROLE: Show add buttons (sequential unlock based on enabled flag)
                if (!step.isCompleted()) {
                    holder.btnAction.setText(step.getActionText());
                    holder.btnAction.setIconResource(step.getActionIcon());
                    
                    // ✅ Check if button is enabled (based on investigation started AND previous steps)
                    if (isInvestigationStarted && step.isEnabled()) {
                        // ✅ ENABLED: Filled blue background
                        holder.btnAction.setEnabled(true);
                        holder.btnAction.setAlpha(1.0f);
                        holder.btnAction.setBackgroundColor(holder.itemView.getContext()
                                .getColor(R.color.electric_blue));
                        holder.btnAction.setTextColor(holder.itemView.getContext()
                                .getColor(android.R.color.white));
                        holder.btnAction.setIconTint(android.content.res.ColorStateList.valueOf(
                                holder.itemView.getContext().getColor(android.R.color.white)));
                        holder.btnAction.setStrokeWidth(0);  // No outline when filled
                        holder.btnAction.setOnClickListener(v -> {
                            if (listener != null) {
                                listener.onStepAction(step);
                            }
                        });
                    } else {
                        // ❌ DISABLED: Outline blue (dimmed)
                        holder.btnAction.setEnabled(false);
                        holder.btnAction.setAlpha(0.6f);
                        holder.btnAction.setBackgroundColor(holder.itemView.getContext()
                                .getColor(android.R.color.transparent));
                        holder.btnAction.setTextColor(holder.itemView.getContext()
                                .getColor(R.color.electric_blue));
                        holder.btnAction.setIconTint(android.content.res.ColorStateList.valueOf(
                                holder.itemView.getContext().getColor(R.color.electric_blue)));
                        holder.btnAction.setStrokeWidth(2);  // Outline stroke
                        holder.btnAction.setStrokeColor(android.content.res.ColorStateList.valueOf(
                                holder.itemView.getContext().getColor(R.color.electric_blue)));
                        holder.btnAction.setOnClickListener(v -> {
                            android.widget.Toast.makeText(
                                holder.itemView.getContext(),
                                "⚠️ Complete previous steps first",
                                android.widget.Toast.LENGTH_SHORT
                            ).show();
                        });
                    }
                } else {
                    holder.btnAction.setVisibility(View.GONE);
                }
            } else {
                // ADMIN ROLE: No buttons (read-only)
                holder.btnAction.setVisibility(View.GONE);
            }
        } else {
            holder.btnAction.setVisibility(View.GONE);
        }
        
        // Show/hide timeline lines
        if (position == 0) {
            holder.vTimelineLineTop.setVisibility(View.INVISIBLE);
        } else {
            holder.vTimelineLineTop.setVisibility(View.VISIBLE);
        }
        
        if (position == steps.size() - 1) {
            holder.vTimelineLineBottom.setVisibility(View.INVISIBLE);
        } else {
            holder.vTimelineLineBottom.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public int getItemCount() {
        return steps != null ? steps.size() : 0;
    }
    
    public void updateSteps(List<InvestigationStep> newSteps) {
        this.steps = newSteps;
        notifyDataSetChanged();
    }
    
    private String getViewButtonText(String tag) {
        if (tag == null) return "View";
        
        switch (tag.toLowerCase()) {
            case "witnesses":
                return "👥 View Witnesses";
            case "suspects":
                return "🚨 View Suspects";
            case "evidence":
                return "📸 View Evidence";
            case "hearings":
                return "📅 View Hearings";
            case "resolution":
                return "✅ View Resolution";
            default:
                return "View Details";
        }
    }
    
    private void handleViewAction(String tag, OnStepActionListener listener) {
        if (tag == null) return;
        
        switch (tag.toLowerCase()) {
            case "witnesses":
                listener.onViewWitnesses(reportId);
                break;
            case "suspects":
                listener.onViewSuspects(reportId);
                break;
            case "evidence":
                listener.onViewEvidence(reportId);
                break;
            case "hearings":
                listener.onViewHearings(reportId);
                break;
            case "resolution":
                listener.onViewResolution(reportId);
                break;
        }
    }
    
    static class StepViewHolder extends RecyclerView.ViewHolder {
        View vTimelineLineTop;
        View vTimelineLineBottom;
        ShapeableImageView ivStatus;
        TextView tvStepTitle;
        TextView tvStepDescription;
        MaterialButton btnAction;
        
        StepViewHolder(@NonNull View itemView) {
            super(itemView);
            vTimelineLineTop = itemView.findViewById(R.id.vTimelineLineTop);
            vTimelineLineBottom = itemView.findViewById(R.id.vTimelineLineBottom);
            ivStatus = itemView.findViewById(R.id.ivStatus);
            tvStepTitle = itemView.findViewById(R.id.tvStepTitle);
            tvStepDescription = itemView.findViewById(R.id.tvStepDescription);
            btnAction = itemView.findViewById(R.id.btnAction);
        }
    }
}
