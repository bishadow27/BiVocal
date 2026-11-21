package com.example.bivoca.ui.topics;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearSnapHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.example.bivoca.R;
import com.example.bivoca.data.model.Topic;
import com.example.bivoca.data.repository.TopicRepository;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.example.bivoca.ui.navigation.LearningBottomNavigation;
import com.example.bivoca.utils.SessionManager;
import java.util.List;
import java.util.EnumMap;

/** Native catalog UI; API loading and Flashcard pagination remain in their existing layers. */
public class ChuDeActivity extends AppCompatActivity {
    private TopicsViewModel viewModel;
    private TopicAdapter adapter, posAdapter;
    private TextView search;
    private boolean resizePending;
    private TopicFilter.Group selected=TopicFilter.Group.ALL;
    private final EnumMap<TopicFilter.Group,Chip> chips=new EnumMap<>(TopicFilter.Group.class);

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(!new SessionManager(this).isLoggedIn()){finish();return;}
        setContentView(R.layout.activity_chu_de);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        NavigationDrawerController.attach(this,NavigationRoute.TOPICS,R.id.buttonHomeMenu);
        LearningBottomNavigation.bind(this,NavigationRoute.TOPICS);
        search=findViewById(R.id.inputTopicSearch);
        if(state!=null) {
            try {selected=TopicFilter.Group.valueOf(state.getString("topicFilter","ALL"));}
            catch(IllegalArgumentException ignored){selected=TopicFilter.Group.ALL;}
            search.setText(state.getString("topicQuery",""));
        }
        adapter=new TopicAdapter(this::openTopic,false);
        posAdapter=new TopicAdapter(this::openTopic,false);
        configureList(R.id.recyclerTopics,adapter);
        configureList(R.id.recyclerTopicPartsOfSpeech,posAdapter);
        viewModel=new ViewModelProvider(this,new ViewModelProvider.Factory(){
            @NonNull @Override @SuppressWarnings("unchecked") public <T extends ViewModel>T create(@NonNull Class<T> type) {
                if(!type.isAssignableFrom(TopicsViewModel.class))throw new IllegalArgumentException("Unknown ViewModel class");
                return (T)new TopicsViewModel(new TopicRepository(getApplicationContext()));
            }
        }).get(TopicsViewModel.class);
        createChips();
        findViewById(R.id.topicsContentViewport).addOnLayoutChangeListener((view,l,t,r,b,ol,ot,or,ob)->{
            if(r-l!=or-ol || b-t!=ob-ot) scheduleResize();
        });
        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){render();}
            @Override public void afterTextChanged(Editable s){}
        });
        viewModel.getLoading().observe(this,v->render());
        viewModel.getError().observe(this,v->render());
        viewModel.getTopics().observe(this,v->{updateChipLabels(v);render();});
        findViewById(R.id.buttonRetryTopics).setOnClickListener(v->viewModel.loadTopics());
        viewModel.ensureTopicsLoaded();
    }

    private void createChips() {
        ChipGroup group=findViewById(R.id.topicsFilterChips);
        for(TopicFilter.Group filter:TopicFilter.Group.values()) {
            Chip chip=filter==TopicFilter.Group.ALL?findViewById(R.id.chipTopicsAll)
                    :(Chip)getLayoutInflater().inflate(R.layout.item_topic_chip,group,false);
            chip.setId(View.generateViewId());chip.setTag(filter.name());chip.setCheckable(true);
            if(filter==TopicFilter.Group.ALL)chip.setId(R.id.chipTopicsAll);
            chips.put(filter,chip);if(filter!=TopicFilter.Group.ALL)group.addView(chip);
            chip.setOnClickListener(button->{
                selected=filter;syncChipSelection();
                ((androidx.core.widget.NestedScrollView)findViewById(R.id.topicsSectionsScroll)).scrollTo(0,0);
                render();
            });
        }
        syncChipSelection();
        updateChipLabels(viewModel.getTopics().getValue());
    }

    private void syncChipSelection() {
        for(TopicFilter.Group filter:TopicFilter.Group.values())chips.get(filter).setChecked(filter==selected);
    }

    private void configureList(int id,TopicAdapter listAdapter) {
        RecyclerView list=findViewById(id);
        list.setLayoutManager(new LinearLayoutManager(this));list.setAdapter(listAdapter);
        list.setNestedScrollingEnabled(false);
        new LinearSnapHelper().attachToRecyclerView(list);
        list.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener(){
            @Override public boolean onInterceptTouchEvent(@NonNull RecyclerView view,@NonNull MotionEvent event){
                int action=event.getActionMasked();
                view.getParent().requestDisallowInterceptTouchEvent(action!=MotionEvent.ACTION_UP&&action!=MotionEvent.ACTION_CANCEL);
                return false;
            }
        });
        list.addOnLayoutChangeListener((view,l,t,r,b,oldL,oldT,oldR,oldB)->{
            if(r-l!=oldR-oldL) scheduleResize();
        });
    }

    private void scheduleResize() {
        if(resizePending)return;
        resizePending=true;
        findViewById(R.id.topicsContentViewport).post(()->{
            resizePending=false;
            resizeList(R.id.recyclerTopics, R.id.topicsSemanticContent, R.id.topicsSemanticHeader, adapter);
            resizeList(R.id.recyclerTopicPartsOfSpeech, R.id.topicsPosContent, R.id.topicsPosHeader, posAdapter);
        });
    }

    private void resizeList(int listId, int contentId, int headerId, TopicAdapter listAdapter) {
        RecyclerView list=findViewById(listId);
        if(list.getWidth()<=0 || listAdapter.getItemCount()==0)return;
        int height=listAdapter.viewportHeight(list);
        if(selected!=TopicFilter.Group.ALL) {
            ViewGroup content=findViewById(contentId);
            View header=findViewById(headerId);
            ViewGroup.MarginLayoutParams headerParams=(ViewGroup.MarginLayoutParams)header.getLayoutParams();
            androidx.core.widget.NestedScrollView scroll=findViewById(R.id.topicsSectionsScroll);
            int available=scroll.getHeight()-scroll.getPaddingTop()-scroll.getPaddingBottom();
            int overhead=content.getPaddingTop()+content.getPaddingBottom()+header.getMeasuredHeight()
                    +headerParams.topMargin+headerParams.bottomMargin;
            // A very short landscape viewport can scroll the outer page to a full, reachable row.
            int minimumRow=height/Math.min(4,listAdapter.getItemCount());
            height=Math.max(minimumRow,available-overhead);
        }
        ViewGroup.LayoutParams params=list.getLayoutParams();
        if(params.height!=height){params.height=height;list.setLayoutParams(params);}
    }

    private void updateChipLabels(List<Topic> topics) {
        for(TopicFilter.Group filter:TopicFilter.Group.values()) {
            int count=TopicFilter.apply(topics,"",filter).size();
            int label=filter==TopicFilter.Group.ALL?R.string.topics16_chip_all
                    :filter==TopicFilter.Group.SEMANTIC?R.string.topics16_chip_semantic
                    :filter==TopicFilter.Group.PART_OF_SPEECH?R.string.topics16_chip_pos
                    :filter==TopicFilter.Group.LIFE?R.string.topics16_chip_life
                    :filter==TopicFilter.Group.WORK?R.string.topics16_chip_work:R.string.topics16_chip_nature;
            chips.get(filter).setText(filter.ordinal()<3?getString(label,count):getString(label));
            chips.get(filter).setEnabled(topics!=null&&(count>0||filter==TopicFilter.Group.ALL));
        }
    }

    private void render() {
        if(viewModel==null||adapter==null)return;
        boolean loading=Boolean.TRUE.equals(viewModel.getLoading().getValue());
        String error=viewModel.getError().getValue();
        boolean failed=!loading&&error!=null&&!error.isEmpty();
        List<Topic> all=viewModel.getTopics().getValue();
        List<Topic> filtered=TopicFilter.apply(all,search.getText().toString(),selected);
        List<Topic> semantic=TopicFilter.apply(filtered,"",TopicFilter.Group.SEMANTIC);
        List<Topic> parts=TopicFilter.apply(filtered,"",TopicFilter.Group.PART_OF_SPEECH);
        adapter.setTopics(semantic);posAdapter.setTopics(parts);
        findViewById(R.id.topicsSemanticFrame).setVisibility(!semantic.isEmpty()?View.VISIBLE:View.GONE);
        View posFrame=findViewById(R.id.topicsPosFrame);
        posFrame.setVisibility(!parts.isEmpty()?View.VISIBLE:View.GONE);
        ViewGroup.MarginLayoutParams posParams=(ViewGroup.MarginLayoutParams)posFrame.getLayoutParams();
        int gap=!semantic.isEmpty()&&!parts.isEmpty()?Math.round(16*getResources().getDisplayMetrics().density):0;
        if(posParams.topMargin!=gap){posParams.topMargin=gap;posFrame.setLayoutParams(posParams);}
        ((TextView)findViewById(R.id.textSemanticGroupCount)).setText(getString(R.string.topics16_group_count,semantic.size()));
        ((TextView)findViewById(R.id.textPosGroupCount)).setText(getString(R.string.topics16_group_count,parts.size()));
        scheduleResize();
        findViewById(R.id.progressBar).setVisibility(loading?View.VISIBLE:View.GONE);
        TextView textError=findViewById(R.id.textError);textError.setText(error);
        textError.setVisibility(failed?View.VISIBLE:View.GONE);
        findViewById(R.id.buttonRetryTopics).setVisibility(failed?View.VISIBLE:View.GONE);
        findViewById(R.id.topicsSectionsScroll).setVisibility(!loading&&!failed?View.VISIBLE:View.GONE);
        TextView empty=findViewById(R.id.textTopicsEmpty);
        empty.setText(all!=null&&all.isEmpty()?R.string.topics16_empty_catalog:R.string.topics16_empty_search);
        empty.setVisibility(!loading&&!failed&&filtered.isEmpty()?View.VISIBLE:View.GONE);
        ((TextView)findViewById(R.id.textTopicsSubtitle)).setText(all==null?getString(loading?R.string.home16_loading:R.string.home16_no_data)
                :getString(R.string.topics16_subtitle,TopicFilter.apply(all,"",TopicFilter.Group.SEMANTIC).size(),TopicFilter.apply(all,"",TopicFilter.Group.PART_OF_SPEECH).size()));
    }

    private void openTopic(Topic topic) {
        Intent intent=new Intent(this,ChiTietChuDeActivity.class);
        intent.putExtra(ChiTietChuDeActivity.EXTRA_TOPIC_CODE,topic.getCode());
        LearningBottomNavigation.open(this,intent);
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle state) {
        state.putString("topicFilter",selected.name());
        state.putString("topicQuery",search==null?"":search.getText().toString());
        super.onSaveInstanceState(state);
    }
}
