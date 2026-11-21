package com.example.bivoca.ui.topics;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bivoca.R;
import com.example.bivoca.data.model.Topic;
import com.example.bivoca.data.repository.FlashcardRepository;
import com.example.bivoca.data.repository.TopicRepository;
import com.example.bivoca.ui.flashcard.TheTuVungActivity;
import com.example.bivoca.ui.navigation.LearningBottomNavigation;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.example.bivoca.ui.quiz.TracNghiemActivity;
import com.example.bivoca.utils.SessionManager;
import java.text.NumberFormat;

/** Catalog metadata and a bounded, read-only preview; learning stays in existing screens. */
public class ChiTietChuDeActivity extends AppCompatActivity {
    public static final String EXTRA_TOPIC_CODE = "topic_code";
    private TopicDetailViewModel viewModel;
    private TopicPreviewAdapter previewAdapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new SessionManager(this).isLoggedIn()) { finish(); return; }
        setContentView(R.layout.activity_chi_tiet_chu_de);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        NavigationDrawerController drawer = NavigationDrawerController.attachSession(
                this, NavigationRoute.TOPIC_DETAIL, R.id.buttonTopicDetailBack, this::backToTopics);
        findViewById(R.id.buttonHomeMenu).setOnClickListener(v -> drawer.open());
        LearningBottomNavigation.bind(this, NavigationRoute.TOPIC_DETAIL);
        previewAdapter = new TopicPreviewAdapter();
        RecyclerView preview = findViewById(R.id.recyclerTopicPreview);
        preview.setLayoutManager(new LinearLayoutManager(this));
        preview.setNestedScrollingEnabled(false);
        preview.setAdapter(previewAdapter);
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
                if (!type.isAssignableFrom(TopicDetailViewModel.class)) throw new IllegalArgumentException("Unknown model");
                return type.cast(new TopicDetailViewModel(new TopicRepository(getApplicationContext()),
                        new FlashcardRepository(getApplicationContext())));
            }
        }).get(TopicDetailViewModel.class);
        findViewById(R.id.buttonRetryTopicMetadata).setOnClickListener(v -> viewModel.retryMetadata());
        findViewById(R.id.buttonRetryTopicPreview).setOnClickListener(v -> viewModel.retryPreview());
        findViewById(R.id.buttonTopicFlashcards).setOnClickListener(v -> openFlashcards());
        findViewById(R.id.buttonTopicQuiz).setOnClickListener(v -> openQuiz());
        viewModel.getTopic().observe(this, value -> renderMetadata());
        viewModel.getMetadataLoading().observe(this, value -> renderMetadata());
        viewModel.getMetadataError().observe(this, value -> renderMetadata());
        viewModel.getPreview().observe(this, value -> renderPreview());
        viewModel.getPreviewLoading().observe(this, value -> renderPreview());
        viewModel.getPreviewError().observe(this, value -> renderPreview());
        viewModel.getShowUnverifiedWarning().observe(this, value -> renderPreview());
        viewModel.ensureLoaded(getIntent().getStringExtra(EXTRA_TOPIC_CODE));
        renderMetadata();
        renderPreview();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        viewModel.ensureLoaded(intent.getStringExtra(EXTRA_TOPIC_CODE));
    }

    private void renderMetadata() {
        Topic topic = viewModel.getTopic().getValue();
        boolean loading = Boolean.TRUE.equals(viewModel.getMetadataLoading().getValue());
        String error = viewModel.getMetadataError().getValue();
        boolean failed = !loading && error != null && !error.isEmpty();
        findViewById(R.id.detailMetadataLoading).setVisibility(loading ? View.VISIBLE : View.GONE);
        TextView errorView = findViewById(R.id.textTopicMetadataError);
        errorView.setText(error);
        errorView.setVisibility(failed ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonRetryTopicMetadata).setVisibility(failed ? View.VISIBLE : View.GONE);
        findViewById(R.id.topicMetadataContent).setVisibility(topic != null ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.textTopicDetailHeader)).setText(
                topic != null ? topic.getName() : getString(R.string.phase_c_detail_title));
        if (topic == null) return;
        ((TextView) findViewById(R.id.textTopicDetailName)).setText(topic.getName());
        ((TextView) findViewById(R.id.textTopicDetailCount)).setText(getString(R.string.topics16_word_count,
                NumberFormat.getIntegerInstance().format(topic.getFlashcardCount())));
        TextView description = findViewById(R.id.textTopicDetailDescription);
        description.setText(topic.getDescription());
        description.setVisibility(hasText(topic.getDescription()) ? View.VISIBLE : View.GONE);
        int color = getColor(TopicPresentation.colorResourceFor(topic.getCode()));
        float density = getResources().getDisplayMetrics().density;
        GradientDrawable tile = new GradientDrawable();
        tile.setColor(getColor(TopicPresentation.backgroundResourceFor(topic.getCode())));
        tile.setStroke(0,color);
        tile.setCornerRadius(16 * density);
        findViewById(R.id.topicDetailIconBox).setBackground(tile);
        ImageView icon = findViewById(R.id.imageTopicDetailIcon);
        TextView glyph = findViewById(R.id.textTopicDetailGlyph);
        boolean pos = topic.isPartOfSpeechGroup();
        icon.setVisibility(pos ? View.GONE : View.VISIBLE);
        glyph.setVisibility(pos ? View.VISIBLE : View.GONE);
        icon.setImageResource(TopicPresentation.iconFor(topic.getCode()));
        icon.setImageTintList(ColorStateList.valueOf(color));
        glyph.setText(TopicPresentation.posGlyph(topic.getCode()));
        glyph.setTextColor(color);
    }

    private void renderPreview() {
        boolean loading = Boolean.TRUE.equals(viewModel.getPreviewLoading().getValue());
        String error = viewModel.getPreviewError().getValue();
        boolean failed = !loading && hasText(error);
        previewAdapter.setCards(viewModel.getPreview().getValue());
        findViewById(R.id.topicPreviewLoading).setVisibility(loading ? View.VISIBLE : View.GONE);
        TextView errorView = findViewById(R.id.textTopicPreviewError);
        errorView.setText(error);
        errorView.setVisibility(failed ? View.VISIBLE : View.GONE);
        findViewById(R.id.buttonRetryTopicPreview).setVisibility(failed ? View.VISIBLE : View.GONE);
        findViewById(R.id.textTopicPreviewEmpty).setVisibility(!loading && !failed
                && previewAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.recyclerTopicPreview).setVisibility(!loading && !failed
                && previewAdapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.textTopicPreviewWarning).setVisibility(!loading && !failed
                && Boolean.TRUE.equals(viewModel.getShowUnverifiedWarning().getValue()) ? View.VISIBLE : View.GONE);
    }

    private void openFlashcards() {
        Topic topic = viewModel.getTopic().getValue();
        if (topic == null) return;
        Intent intent = new Intent(this, TheTuVungActivity.class);
        intent.putExtra(TheTuVungActivity.EXTRA_TOPIC_CODE, topic.getCode());
        intent.putExtra(TheTuVungActivity.EXTRA_TOPIC_NAME, topic.getName());
        LearningBottomNavigation.open(this,intent);
    }

    private void openQuiz() {
        Topic topic = viewModel.getTopic().getValue();
        if (topic == null) return;
        Intent intent = new Intent(this, TracNghiemActivity.class);
        intent.putExtra(TracNghiemActivity.EXTRA_TOPIC_CODE, topic.getCode());
        LearningBottomNavigation.open(this,intent);
    }

    private void backToTopics() {
        Intent intent = new Intent(this, ChuDeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        LearningBottomNavigation.open(this,intent);
        finish();
    }

    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
