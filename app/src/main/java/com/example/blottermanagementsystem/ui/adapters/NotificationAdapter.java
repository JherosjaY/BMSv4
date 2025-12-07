package com.example.blottermanagementsystem.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.blottermanagementsystem.R;
import com.example.blottermanagementsystem.data.entity.Notification;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
    private List<Notification> notificationList;
    private OnNotificationClickListener listener;
    private OnNotificationLongClickListener longClickListener;
    private OnViewDetailsClickListener viewDetailsListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
    private boolean isSelectionMode = false;
    private List<Integer> selectedNotifications = new ArrayList<>();
    
    public interface OnNotificationClickListener {
        void onClick(Notification notification);
    }
    
    public interface OnNotificationLongClickListener {
        void onLongClick(Notification notification);
    }
    
    public interface OnViewDetailsClickListener {
        void onViewDetails(Notification notification);
    }
    
    public interface OnNotificationCardClickListener {
        void onCardClick(Notification notification);
    }
    
    public NotificationAdapter(List<Notification> notificationList, 
                              OnNotificationClickListener listener,
                              OnNotificationLongClickListener longClickListener) {
        this.notificationList = notificationList;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }
    
    public void setViewDetailsListener(OnViewDetailsClickListener viewDetailsListener) {
        this.viewDetailsListener = viewDetailsListener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());
        holder.tvDate.setText(dateFormat.format(new Date(notification.getTimestamp())));
        
        // Show/hide unread indicator and checkbox (auto-hide/replace)
        if (isSelectionMode) {
            // Selection mode: Hide blue dot, show checkbox
            if (holder.unreadIndicator != null) {
                holder.unreadIndicator.setVisibility(View.GONE);
            }
            if (holder.checkBox != null) {
                holder.checkBox.setVisibility(View.VISIBLE);
                holder.checkBox.setChecked(selectedNotifications.contains(notification.getId()));
                // Prevent checkbox from intercepting clicks
                holder.checkBox.setOnClickListener(v -> listener.onClick(notification));
            }
        } else {
            // Normal mode: Show blue dot if unread, hide checkbox
            if (holder.unreadIndicator != null) {
                holder.unreadIndicator.setVisibility(notification.isRead() ? 
                    View.GONE : View.VISIBLE);
            }
            if (holder.checkBox != null) {
                holder.checkBox.setVisibility(View.GONE);
            }
        }
        
        // Slightly different opacity for read notifications
        holder.itemView.setAlpha(notification.isRead() ? 0.7f : 1.0f);
        
        // Card click: Open full message dialog (if not in selection mode)
        holder.itemView.setOnClickListener(v -> {
            if (isSelectionMode) {
                listener.onClick(notification);
            } else {
                // Open full message dialog
                if (viewDetailsListener != null) {
                    viewDetailsListener.onViewDetails(notification);
                }
            }
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onLongClick(notification);
            }
            return true;
        });
        
        // View Details click listener (same as card click)
        if (holder.tvViewDetails != null) {
            holder.tvViewDetails.setOnClickListener(v -> {
                if (viewDetailsListener != null) {
                    viewDetailsListener.onViewDetails(notification);
                }
            });
        }
    }
    
    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            selectedNotifications.clear();
        }
    }
    
    public void setSelectedNotifications(List<Integer> selectedNotifications) {
        this.selectedNotifications = selectedNotifications;
    }
    
    @Override
    public int getItemCount() {
        return notificationList.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvDate, tvViewDetails;
        View unreadIndicator;
        CheckBox checkBox;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvDate = itemView.findViewById(R.id.tvTime);
            tvViewDetails = itemView.findViewById(R.id.tvViewDetails);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
