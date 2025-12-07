package com.example.blottermanagementsystem.ui.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.model.InvestigationStep;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * ✅ INVESTIGATION ACTIONS ADAPTER
 * Displays investigation action steps with simple numbering (1, 2, 3, 4, 5)
 * Separate from Case Timeline which uses checkmarks, hourglasses, circles
 */
public class InvestigationActionAdapter extends RecyclerView.Adapter<InvestigationActionAdapter.ActionViewHolder> {
    
    private List<InvestigationStep> steps;
    private OnActionListener listener;
    private boolean isInvestigationStarted = false;
    private String userRole = "OFFICER";
    private int reportId = -1;
    
    public interface OnActionListener {
        void onStepAction(InvestigationStep step);
        void onViewWitnesses(int reportId);
        void onViewSuspects(int reportId);
        void onViewEvidence(int reportId);
        void onViewHearings(int reportId);
        void onViewResolution(int reportId);
        void onViewStepDetails(String stepTag, int reportId);
    }
    
    public InvestigationActionAdapter(List<InvestigationStep> steps, OnActionListener listener, boolean isInvestigationStarted) {
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
    public ActionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_investigation_action, parent, false);
        return new ActionViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ActionViewHolder holder, int position) {
        InvestigationStep step = steps.get(position);
        
        // Set step title and description
        holder.tvStepTitle.setText(step.getTitle());
        holder.tvStepDescription.setText(step.getDescription());
        
        // ✅ Set step number (1, 2, 3, 4, 5)
        holder.tvStepNumber.setText(String.valueOf(position + 1));
        
        // ✅ DIM ENTIRE STEP (texts + button) when disabled
        boolean isStepEnabled = isInvestigationStarted && step.isEnabled();
        if ("OFFICER".equalsIgnoreCase(userRole) && !step.isCompleted()) {
            if (!isStepEnabled) {
                // ❌ DISABLED: Dim all texts
                holder.tvStepNumber.setAlpha(0.6f);
                holder.tvStepTitle.setAlpha(0.6f);
                holder.tvStepDescription.setAlpha(0.6f);
            } else {
                // ✅ ENABLED: Full opacity
                holder.tvStepNumber.setAlpha(1.0f);
                holder.tvStepTitle.setAlpha(1.0f);
                holder.tvStepDescription.setAlpha(1.0f);
            }
        } else {
            // Full opacity for completed steps and other roles
            holder.tvStepNumber.setAlpha(1.0f);
            holder.tvStepTitle.setAlpha(1.0f);
            holder.tvStepDescription.setAlpha(1.0f);
        }
        
        // Show/hide action button based on role
        if (step.getActionText() != null) {
            holder.btnAction.setVisibility(View.VISIBLE);
            
            if ("USER".equalsIgnoreCase(userRole)) {
                // USER ROLE: Show view buttons (always enabled, read-only)
                String buttonText = getViewButtonText(step.getTag());
                holder.btnAction.setText(buttonText);
                holder.btnAction.setIconResource(R.drawable.ic_visibility);
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
                    // ✅ STEP NOT COMPLETED: Show action button
                    holder.btnAction.setVisibility(View.VISIBLE);
                    holder.tvViewDetails.setVisibility(View.GONE);
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
                        holder.btnAction.setIconTint(ColorStateList.valueOf(
                                holder.itemView.getContext().getColor(android.R.color.white)));
                        holder.btnAction.setStrokeWidth(0);
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
                        holder.btnAction.setIconTint(ColorStateList.valueOf(
                                holder.itemView.getContext().getColor(R.color.electric_blue)));
                        holder.btnAction.setStrokeWidth(2);
                        holder.btnAction.setStrokeColor(ColorStateList.valueOf(
                                holder.itemView.getContext().getColor(R.color.electric_blue)));
                        holder.btnAction.setOnClickListener(v -> {
                            Toast.makeText(
                                holder.itemView.getContext(),
                                "⚠️ Complete previous steps first",
                                Toast.LENGTH_SHORT
                            ).show();
                        });
                    }
                } else {
                    // ✅ STEP COMPLETED: Show "View [Item]" button (filled blue)
                    holder.btnAction.setVisibility(View.VISIBLE);
                    holder.tvViewDetails.setVisibility(View.GONE);
                    
                    // Change button text to "View [Item]"
                    String viewButtonText = getViewButtonText(step.getTag());
                    holder.btnAction.setText(viewButtonText);
                    holder.btnAction.setIconResource(R.drawable.ic_visibility);
                    
                    // Style as filled blue button (enabled state)
                    holder.btnAction.setEnabled(true);
                    holder.btnAction.setAlpha(1.0f);
                    holder.btnAction.setBackgroundColor(holder.itemView.getContext()
                            .getColor(R.color.electric_blue));
                    holder.btnAction.setTextColor(holder.itemView.getContext()
                            .getColor(android.R.color.white));
                    holder.btnAction.setIconTint(ColorStateList.valueOf(
                            holder.itemView.getContext().getColor(android.R.color.white)));
                    holder.btnAction.setStrokeWidth(0);
                    
                    // ✅ Click to view details - with debug logging
                    holder.btnAction.setOnClickListener(v -> {
                        android.util.Log.d("InvestigationAdapter", "View Details clicked for step: " + step.getTag() + ", reportId: " + reportId);
                        if (listener != null && reportId != -1) {
                            listener.onViewStepDetails(step.getTag(), reportId);
                        } else {
                            android.util.Log.e("InvestigationAdapter", "Listener is null or reportId is -1");
                        }
                    });
                }
            } else {
                // ADMIN ROLE: No buttons (read-only)
                holder.btnAction.setVisibility(View.GONE);
                holder.tvViewDetails.setVisibility(View.GONE);
            }
        } else {
            holder.btnAction.setVisibility(View.GONE);
            holder.tvViewDetails.setVisibility(View.GONE);
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
            case "record_witness":
                return "View Witnesses";
            case "suspects":
            case "identify_suspect":
                return "View Suspects";
            case "evidence":
                return "View Evidence";
            case "hearings":
            case "schedule_hearing":
                return "View Hearings";
            case "resolution":
            case "document_resolution":
                return "View Resolution";
            default:
                return "View Details";
        }
    }
    
    private void handleViewAction(String tag, OnActionListener listener) {
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
    
    static class ActionViewHolder extends RecyclerView.ViewHolder {
        View vTimelineLineTop;
        View vTimelineLineBottom;
        TextView tvStepNumber;
        TextView tvStepTitle;
        TextView tvStepDescription;
        MaterialButton btnAction;
        TextView tvViewDetails;
        
        ActionViewHolder(@NonNull View itemView) {
            super(itemView);
            vTimelineLineTop = itemView.findViewById(R.id.vTimelineLineTop);
            vTimelineLineBottom = itemView.findViewById(R.id.vTimelineLineBottom);
            tvStepNumber = itemView.findViewById(R.id.tvStepNumber);
            tvStepTitle = itemView.findViewById(R.id.tvStepTitle);
            tvStepDescription = itemView.findViewById(R.id.tvStepDescription);
            btnAction = itemView.findViewById(R.id.btnAction);
            tvViewDetails = itemView.findViewById(R.id.tvViewDetails);
        }
    }
}
