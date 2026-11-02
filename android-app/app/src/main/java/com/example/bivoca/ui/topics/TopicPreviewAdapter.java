package com.example.bivoca.ui.topics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bivoca.R;
import com.example.bivoca.data.model.Flashcard;
import java.util.ArrayList;
import java.util.List;

/** Read-only rendering of the at-most-three cards selected by the Detail ViewModel. */
public final class TopicPreviewAdapter extends RecyclerView.Adapter<TopicPreviewAdapter.Holder> {
    private final List<Flashcard> cards = new ArrayList<>();
    public TopicPreviewAdapter() { setHasStableIds(true); }
    public void setCards(List<Flashcard> values) {
        cards.clear();
        if (values != null) cards.addAll(values.subList(0, Math.min(3, values.size())));
        notifyDataSetChanged();
    }
    @Override public long getItemId(int position) { return cards.get(position).getId(); }
    @Override public int getItemCount() { return cards.size(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic_preview, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Flashcard card = cards.get(position);
        ((TextView) holder.itemView.findViewById(R.id.textPreviewEnglish)).setText(card.getEnglish());
        ((TextView) holder.itemView.findViewById(R.id.textPreviewVietnamese)).setText(card.getVietnamese());
        optional(holder.itemView.findViewById(R.id.textPreviewPronunciation), card.getPronunciation());
        StringBuilder metadata = new StringBuilder();
        append(metadata, card.getPartOfSpeech());
        append(metadata, card.getLevel());
        optional(holder.itemView.findViewById(R.id.textPreviewMetadata), metadata.toString());
    }
    private static void append(StringBuilder value, String part) {
        if (part == null || part.trim().isEmpty()) return;
        if (value.length() > 0) value.append(" · ");
        value.append(part);
    }
    private static void optional(TextView view, String value) {
        view.setText(value);
        view.setVisibility(value != null && !value.trim().isEmpty() ? View.VISIBLE : View.GONE);
    }
    static final class Holder extends RecyclerView.ViewHolder {
        Holder(View item) { super(item); }
    }
}
