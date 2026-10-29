package com.example.bivoca.data.model;

import java.util.List;

public final class FlashcardPage {
    private List<Flashcard> items;
    private int page;
    private int limit;
    private int total;
    private int totalPages;

    public FlashcardPage() { }
    public FlashcardPage(List<Flashcard> items, int page, int limit, int total, int totalPages) {
        this.items=items;this.page=page;this.limit=limit;this.total=total;this.totalPages=totalPages;
    }

    public List<Flashcard> getItems() { return items; }
    public int getPage() { return page; }
    public int getLimit() { return limit; }
    public int getTotal() { return total; }
    public int getTotalPages() { return totalPages; }
}
