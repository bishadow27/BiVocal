package com.example.bivoca.ui.leaderboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bivoca.R;
import com.example.bivoca.data.model.LeaderboardUser;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardUser> users = new ArrayList<>();
    private final int currentUserId;

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
        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            int color = android.graphics.Color.parseColor(user.getUserId() == currentUserId ? "#173A66" : "#111C31");
            ((com.google.android.material.card.MaterialCardView) holder.itemView).setCardBackgroundColor(color);
        }

        // Rank representation
        int rank = user.getRank();
        if (rank == 1) {
            holder.textRank.setText("🥇");
            holder.textRank.setTextSize(22);
        } else if (rank == 2) {
            holder.textRank.setText("🥈");
            holder.textRank.setTextSize(22);
        } else if (rank == 3) {
            holder.textRank.setText("🥉");
            holder.textRank.setTextSize(22);
        } else {
            holder.textRank.setText("#" + rank);
            holder.textRank.setTextSize(15);
        }

        // Avatar letter
        String displayName = user.getDisplayName();
        String letter = "U";
        if (!displayName.trim().isEmpty()) {
            letter = displayName.trim().substring(0, 1).toUpperCase();
        }
        holder.textUserAvatar.setText(letter);

        // Display name & username
        holder.textDisplayName.setText(displayName);
        holder.textUsername.setText("@" + user.getUsername());

        // Level & Streak
        holder.textUserLevel.setText("Lv. " + user.getLevel());
        holder.textUserStreak.setText("🔥 " + user.getCurrentStreak());

        // XP
        holder.textUserXp.setText(user.getTotalXp() + " XP");
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textRank;
        final TextView textUserAvatar;
        final TextView textDisplayName;
        final TextView textUsername;
        final TextView textUserLevel;
        final TextView textUserStreak;
        final TextView textUserXp;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textRank = itemView.findViewById(R.id.textRank);
            textUserAvatar = itemView.findViewById(R.id.textUserAvatar);
            textDisplayName = itemView.findViewById(R.id.textDisplayName);
            textUsername = itemView.findViewById(R.id.textUsername);
            textUserLevel = itemView.findViewById(R.id.textUserLevel);
            textUserStreak = itemView.findViewById(R.id.textUserStreak);
            textUserXp = itemView.findViewById(R.id.textUserXp);
        }
    }
}
