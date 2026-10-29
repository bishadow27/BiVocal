package com.example.bivoca.ui.search;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bivoca.R;
import com.example.bivoca.data.model.FlashcardPage;
import com.example.bivoca.data.repository.VocabularyRepository;

public final class VocabularySearchActivity extends AppCompatActivity {
    public static final String EXTRA_TOPIC_CODE = "topicCode";
    public static final String EXTRA_TOPIC_NAME = "topicName";
    private VocabularySearchViewModel viewModel;
    private EditText input;
    private Spinner level;
    private ProgressBar loading;
    private TextView status;
    private Button next, retry;
    private VocabularySearchAdapter adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_vocabulary_search);
        input=findViewById(R.id.inputVocabularySearch); level=findViewById(R.id.spinnerVocabularyLevel);
        loading=findViewById(R.id.progressVocabularySearch); status=findViewById(R.id.textVocabularySearchStatus);
        next=findViewById(R.id.buttonVocabularyNext); retry=findViewById(R.id.buttonVocabularyRetry);
        RecyclerView list=findViewById(R.id.recyclerVocabularySearch);
        adapter=new VocabularySearchAdapter(); list.setLayoutManager(new LinearLayoutManager(this)); list.setAdapter(adapter);
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, R.layout.item_spinner_dark,
                R.id.textSpinnerItem, new String[]{"Tất cả cấp độ", "A1", "A2", "B1", "B2"});
        levelAdapter.setDropDownViewResource(R.layout.item_spinner_dark);
        level.setAdapter(levelAdapter);
        viewModel=new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull @Override @SuppressWarnings("unchecked") public <T extends ViewModel> T create(@NonNull Class<T> type) {
                return (T) new VocabularySearchViewModel(new VocabularyRepository());
            }
        }).get(VocabularySearchViewModel.class);
        String topicCode = getIntent().getStringExtra(EXTRA_TOPIC_CODE);
        String topicName = getIntent().getStringExtra(EXTRA_TOPIC_NAME);
        viewModel.setTopicCode(topicCode);
        if (topicCode != null && !topicCode.trim().isEmpty()) {
            TextView title = findViewById(R.id.textVocabularySearchTitle);
            title.setText(topicName == null || topicName.trim().isEmpty() ? "TỪ VỰNG THEO CHỦ ĐỀ" : topicName.trim());
            viewModel.search("", null, 1);
        }
        findViewById(R.id.buttonVocabularyBack).setOnClickListener(view -> finish());
        findViewById(R.id.buttonVocabularySearch).setOnClickListener(view -> submit());
        retry.setOnClickListener(view -> submit());
        input.setOnEditorActionListener((view, action, event) -> { if (action == EditorInfo.IME_ACTION_SEARCH) { submit(); return true; } return false; });
        next.setOnClickListener(view -> viewModel.nextPage());
        viewModel.getLoading().observe(this, value -> {
            boolean pending = Boolean.TRUE.equals(value);
            loading.setVisibility(pending ? View.VISIBLE : View.GONE);
            retry.setEnabled(!pending);
        });
        viewModel.getError().observe(this, value -> {
            boolean failed = value != null && !value.trim().isEmpty();
            retry.setVisibility(failed ? View.VISIBLE : View.GONE);
            if (failed) {
                status.setText(value); status.setVisibility(View.VISIBLE);
                next.setVisibility(View.GONE);
            }
        });
        viewModel.getResult().observe(this, this::render);
    }

    private void submit() { viewModel.search(input.getText().toString(), level.getSelectedItemPosition() == 0 ? null : level.getSelectedItem().toString(), 1); }
    private void render(FlashcardPage page) {
        retry.setVisibility(View.GONE);
        adapter.setItems(page.getItems());
        status.setText(page.getTotal() == 0 ? "Không tìm thấy từ phù hợp." : "Tìm thấy " + page.getTotal() + " kết quả • Trang " + page.getPage() + "/" + page.getTotalPages());
        status.setVisibility(View.VISIBLE); next.setVisibility(page.getPage() < page.getTotalPages() ? View.VISIBLE : View.GONE);
    }
}
