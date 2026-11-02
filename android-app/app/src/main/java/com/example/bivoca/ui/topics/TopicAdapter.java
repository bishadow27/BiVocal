package com.example.bivoca.ui.topics;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bivoca.R;
import com.example.bivoca.data.model.Topic;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class TopicAdapter extends RecyclerView.Adapter<TopicAdapter.TopicViewHolder> {
    private final List<Topic> topics=new ArrayList<>();
    private final OnTopicClickListener listener;
    private final boolean showSectionHeadings;
    private int uniformRowHeight;
    private int measuredWidth = -1;
    private int semanticCount, posCount;
    public interface OnTopicClickListener {void onTopicClick(Topic topic);}
    public TopicAdapter(OnTopicClickListener listener){this(listener,true);}
    public TopicAdapter(OnTopicClickListener listener,boolean showSectionHeadings){this.listener=listener;this.showSectionHeadings=showSectionHeadings;setHasStableIds(true);setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);}
    public void setTopics(List<Topic> newTopics) {
        List<Topic> next = newTopics == null ? java.util.Collections.emptyList() : newTopics;
        if (topics.equals(next)) return;
        topics.clear(); topics.addAll(next);
        semanticCount = 0; posCount = 0;
        for (Topic topic : topics) { if (topic.isPartOfSpeechGroup()) posCount++; else semanticCount++; }
        uniformRowHeight = 0; measuredWidth = -1;
        notifyDataSetChanged();
    }
    @Override public long getItemId(int position){return topics.get(position).getId();}
    @Override public int getItemCount(){return topics.size();}
    @NonNull @Override public TopicViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){
        return new TopicViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_topic,parent,false));
    }
    @Override public void onBindViewHolder(@NonNull TopicViewHolder holder,int position){
        Topic topic=topics.get(position);
        boolean pos=topic.isPartOfSpeechGroup();
        boolean first=position==0||topics.get(position-1).isPartOfSpeechGroup()!=pos;
        holder.itemView.findViewById(R.id.topicGroupHeading).setVisibility(showSectionHeadings&&first?View.VISIBLE:View.GONE);
        ViewGroup.LayoutParams rowParams=holder.itemView.getLayoutParams();
        rowParams.height=uniformRowHeight>0?uniformRowHeight:ViewGroup.LayoutParams.WRAP_CONTENT;
        holder.itemView.setLayoutParams(rowParams);
        View card=holder.itemView.findViewById(R.id.cardTopic);
        ViewGroup.MarginLayoutParams cardParams=(ViewGroup.MarginLayoutParams)card.getLayoutParams();
        cardParams.height=uniformRowHeight>0?uniformRowHeight-cardParams.bottomMargin:ViewGroup.LayoutParams.WRAP_CONTENT;
        card.setLayoutParams(cardParams);
        holder.text(R.id.textTopicGroupHeader).setText(pos?R.string.topics16_pos_heading:R.string.topics16_semantic_heading);
        int count=pos?posCount:semanticCount;
        holder.text(R.id.textTopicGroupCount).setText(holder.itemView.getContext().getString(R.string.topics16_group_count,count));
        holder.text(R.id.textTopicName).setText(topic.getName());
        TextView description=holder.text(R.id.textTopicDescription);
        description.setText(topic.getDescription());
        description.setVisibility(topic.getDescription()==null||topic.getDescription().trim().isEmpty()?View.GONE:View.VISIBLE);
        holder.text(R.id.textFlashcardCount).setText(holder.itemView.getContext().getString(R.string.topics16_word_count,NumberFormat.getIntegerInstance().format(topic.getFlashcardCount())));
        int color=holder.itemView.getContext().getColor(TopicPresentation.colorResourceFor(topic.getCode()));
        float density=holder.itemView.getResources().getDisplayMetrics().density;
        GradientDrawable tile=new GradientDrawable();tile.setColor(holder.itemView.getContext().getColor(TopicPresentation.backgroundResourceFor(topic.getCode())));
        tile.setStroke(0,color);tile.setCornerRadius(12*density);
        holder.itemView.findViewById(R.id.topicIconBox).setBackground(tile);
        ImageView icon=holder.itemView.findViewById(R.id.imageTopicIcon);
        TextView glyph=holder.text(R.id.textTopicPosGlyph);
        icon.setVisibility(pos?View.GONE:View.VISIBLE);glyph.setVisibility(pos?View.VISIBLE:View.GONE);
        icon.setImageResource(TopicPresentation.iconFor(topic.getCode()));icon.setImageTintList(ColorStateList.valueOf(color));
        glyph.setTextColor(color);glyph.setText(TopicPresentation.posGlyph(topic.getCode()));
        holder.itemView.findViewById(R.id.cardTopic).setOnClickListener(v->listener.onTopicClick(topic));
    }
    /** Cached natural height: measure the small catalog once per data/width change, never per layout. */
    public int viewportHeight(RecyclerView parent) {
        int width=parent.getWidth()-parent.getPaddingLeft()-parent.getPaddingRight();
        if (width<=0 || topics.isEmpty()) return 0;
        if (width!=measuredWidth || uniformRowHeight<=0) {
            int height=0;
            uniformRowHeight=0;
            TopicViewHolder sample=onCreateViewHolder(parent,0);
            for(int i=0;i<topics.size();i++) {
                onBindViewHolder(sample,i);
                sample.itemView.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                height=Math.max(height,sample.itemView.getMeasuredHeight());
            }
            uniformRowHeight=height;
            measuredWidth=width;
            notifyDataSetChanged();
        }
        return uniformRowHeight*Math.min(4,getItemCount());
    }
    static class TopicViewHolder extends RecyclerView.ViewHolder {
        TopicViewHolder(@NonNull View item){super(item);}
        TextView text(int id){return itemView.findViewById(id);}
    }
}
