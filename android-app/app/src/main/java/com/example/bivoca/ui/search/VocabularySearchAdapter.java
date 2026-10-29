package com.example.bivoca.ui.search;

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

final class VocabularySearchAdapter extends RecyclerView.Adapter<VocabularySearchAdapter.Holder> {
    private final List<Flashcard> items = new ArrayList<>();
    void setItems(List<Flashcard> values) { items.clear(); if (values != null) items.addAll(values); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vocabulary_search, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Flashcard card = items.get(position);
        holder.word.setText(card.getEnglish()); holder.meaning.setText(card.getVietnamese());
        StringBuilder metadata = new StringBuilder();
        append(metadata, card.getPronunciation()); append(metadata, card.getPartOfSpeech()); append(metadata, card.getLevel());
        if (card.isImportedUnverified()) append(metadata, "Chưa kiểm duyệt ngữ nghĩa");
        holder.metadata.setText(metadata); holder.metadata.setVisibility(metadata.length() == 0 ? View.GONE : View.VISIBLE);
        holder.example.setText(card.getExample());
        holder.example.setVisibility(card.getExample() == null || card.getExample().trim().isEmpty() ? View.GONE : View.VISIBLE);
    }
    private static void append(StringBuilder target, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (target.length() > 0) target.append("  •  "); target.append(value.trim());
    }
    @Override public int getItemCount() { return items.size(); }
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView word, meaning, metadata, example;
        Holder(View view) { super(view); word=view.findViewById(R.id.textSearchWord); meaning=view.findViewById(R.id.textSearchMeaning);
            metadata=view.findViewById(R.id.textSearchMetadata); example=view.findViewById(R.id.textSearchExample); }
    }
}
