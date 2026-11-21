package com.example.bivoca.ui.leaderboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bivoca.R;
import com.example.bivoca.data.model.LeaderboardUser;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardUser> users = new ArrayList<>();
    private final int currentUserId;
    private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

    public LeaderboardAdapter(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setUsers(List<LeaderboardUser> newUsers) {
        users.clear();
        if (newUsers != null) {
            users.addAll(newUsers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardUser user = users.get(position);
        boolean isCurrentUser = (user.getUserId() == currentUserId);

        // Background highlight for self
        holder.itemView.setBackgroundResource(
                isCurrentUser ? R.drawable.lb_light_row_self : R.drawable.lb_light_row
        );

        // Rank number
        holder.textRank.setText(String.valueOf(user.getRank()));

        // Avatar letters (initials)
        String displayName = user.getDisplayName();
        String initials = getInitials(displayName);
        holder.textUserAvatar.setText(initials);
        int[] avatarBackgrounds = {R.drawable.lb_light_avatar_blue, R.drawable.lb_light_avatar_violet,
                R.drawable.lb_light_avatar_green, R.drawable.lb_light_avatar_coral};
        int[] avatarColors = {R.color.lb_light_cobalt, R.color.lb_light_violet_ink,
                R.color.lb_light_green_ink, R.color.lb_light_coral_ink};
        int palette = position % avatarBackgrounds.length;
        holder.textUserAvatar.setBackgroundResource(isCurrentUser ? R.drawable.lb_light_avatar_self : avatarBackgrounds[palette]);
        holder.textUserAvatar.setTextColor(holder.itemView.getContext().getColor(
                isCurrentUser ? R.color.lb_light_surface : avatarColors[palette]));
        holder.textRank.setTextColor(holder.itemView.getContext().getColor(
                isCurrentUser ? R.color.lb_light_cobalt : R.color.lb_light_secondary));
        holder.textUserXp.setTextColor(holder.itemView.getContext().getColor(
                isCurrentUser ? R.color.lb_light_cobalt : R.color.lb_light_text));

        // Display name
        holder.textDisplayName.setText(displayName);

        // Self badge
        if (holder.textUserTagSelf != null) {
            holder.textUserTagSelf.setVisibility(isCurrentUser ? View.VISIBLE : View.GONE);
        }

        // Score with diamond format
        holder.textUserXp.setText(numberFormat.format(user.getTotalXp()));
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textRank;
        final TextView textUserAvatar;
        final TextView textDisplayName;
        final TextView textUserTagSelf;
        final TextView textUserXp;
        final TextView textUsername;
        final TextView textUserLevel;
        final TextView textUserStreak;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textRank = itemView.findViewById(R.id.textRank);
            textUserAvatar = itemView.findViewById(R.id.textUserAvatar);
            textDisplayName = itemView.findViewById(R.id.textDisplayName);
            textUserTagSelf = itemView.findViewById(R.id.textUserTagSelf);
            textUserXp = itemView.findViewById(R.id.textUserXp);
            textUsername = itemView.findViewById(R.id.textUsername);
            textUserLevel = itemView.findViewById(R.id.textUserLevel);
            textUserStreak = itemView.findViewById(R.id.textUserStreak);
        }
    }
}
