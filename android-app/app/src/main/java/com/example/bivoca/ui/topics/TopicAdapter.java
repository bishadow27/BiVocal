package com.example.bivoca.ui.topics;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
    public interface OnTopicClickListener {void onTopicClick(Topic topic);}
    public TopicAdapter(OnTopicClickListener listener){this(listener,true);}
    public TopicAdapter(OnTopicClickListener listener,boolean showSectionHeadings){this.listener=listener;this.showSectionHeadings=showSectionHeadings;setHasStableIds(true);}
    public void setTopics(List<Topic> newTopics){topics.clear();if(newTopics!=null)topics.addAll(newTopics);notifyDataSetChanged();}
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
        int count=0;for(Topic row:topics)if(row.isPartOfSpeechGroup()==pos)count++;
        holder.text(R.id.textTopicGroupCount).setText(holder.itemView.getContext().getString(R.string.topics16_group_count,count));
        holder.text(R.id.textTopicName).setText(topic.getName());
        TextView description=holder.text(R.id.textTopicDescription);
        description.setText(topic.getDescription());
        description.setVisibility(topic.getDescription()==null||topic.getDescription().trim().isEmpty()?View.GONE:View.VISIBLE);
        holder.text(R.id.textFlashcardCount).setText(holder.itemView.getContext().getString(R.string.topics16_word_count,NumberFormat.getIntegerInstance().format(topic.getFlashcardCount())));
        int color=colorFor(topic.getCode());
        float density=holder.itemView.getResources().getDisplayMetrics().density;
        GradientDrawable tile=new GradientDrawable();tile.setColor((color&0x00FFFFFF)|0x1A000000);
        tile.setStroke(Math.max(1,Math.round(density)),(color&0x00FFFFFF)|0x50000000);tile.setCornerRadius(12*density);
        holder.itemView.findViewById(R.id.topicIconBox).setBackground(tile);
        ImageView icon=holder.itemView.findViewById(R.id.imageTopicIcon);
        TextView glyph=holder.text(R.id.textTopicPosGlyph);
        icon.setVisibility(pos?View.GONE:View.VISIBLE);glyph.setVisibility(pos?View.VISIBLE:View.GONE);
        icon.setImageResource(iconFor(topic.getCode()));icon.setImageTintList(ColorStateList.valueOf(color));
        glyph.setTextColor(color);glyph.setText(posGlyph(topic.getCode()));
        holder.itemView.findViewById(R.id.cardTopic).setOnClickListener(v->listener.onTopicClick(topic));
    }
    /** Measure only small catalog rows; never loads flashcards. Equal rows give a four-item viewport at every font size. */
    public int viewportHeight(RecyclerView parent) {
        int previous=uniformRowHeight;uniformRowHeight=0;
        int height=0,width=parent.getWidth()-parent.getPaddingLeft()-parent.getPaddingRight();
        for(int i=0;i<topics.size();i++) {
            TopicViewHolder sample=onCreateViewHolder(parent,0);onBindViewHolder(sample,i);
            sample.itemView.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
            height=Math.max(height,sample.itemView.getMeasuredHeight());
        }
        uniformRowHeight=height;
        if(previous!=height)notifyDataSetChanged();
        return height*Math.min(4,getItemCount());
    }
    private static String posGlyph(String code){
        if(code==null)return "";
        switch(code){case "POS_NOUN":return "N";case "POS_VERB":return "V";case "POS_ADJECTIVE":return "Adj";case "POS_ADVERB":return "Adv";case "POS_PREPOSITION":return "Prep";case "POS_CONJUNCTION":return "Conj";case "POS_PRONOUN":return "Pron";default:return "Aa";}
    }
    private static int colorFor(String code){
        if(code==null)return Color.rgb(56,189,248);
        switch(code){case "FOOD":case "HEALTH":case "EMOTIONS":return Color.rgb(248,113,113);
            case "ANIMALS":case "WEATHER":case "FINANCE":return Color.rgb(251,191,36);
            case "SCHOOL":case "CULTURE":case "ARTS":case "LANGUAGE":return Color.rgb(167,139,250);
            case "WORK":case "NATURE":case "SCIENCE":return Color.rgb(45,212,191);
            case "FAMILY":case "RELATIONSHIPS":case "CLOTHING":return Color.rgb(244,114,182);
            default:return Color.rgb(56,189,248);}
    }
    private static int iconFor(String code){
        if(code==null)return R.drawable.topics16_book;
        switch(code){
            case "FOOD":return R.drawable.topics16_food;case "ANIMALS":return R.drawable.topics16_animal;
            case "TRAVEL":return R.drawable.topics16_travel;case "SCHOOL":return R.drawable.topics16_school;
            case "FAMILY":case "RELATIONSHIPS":return R.drawable.topics16_people;
            case "TECHNOLOGY":return R.drawable.topics16_technology;case "WORK":return R.drawable.topics16_work;
            case "HEALTH":case "EMOTIONS":return R.drawable.topics16_health;case "SHOPPING":return R.drawable.topics16_shopping;
            case "WEATHER":return R.drawable.topics16_weather;case "HOME":return R.drawable.home16_icon_home;
            case "PERSONAL":return R.drawable.home16_icon_profile;case "DAILY_LIFE":case "TIME_CALENDAR":return R.drawable.home16_icon_review;
            case "TRANSPORTATION":return R.drawable.topics16_transport;case "NATURE":return R.drawable.topics16_nature;
            case "FINANCE":return R.drawable.topics16_finance;case "SCIENCE":return R.drawable.topics16_science;
            case "LAW_GOVERNMENT":return R.drawable.topics16_law;case "COMMUNICATION":return R.drawable.topics16_communication;
            case "CLOTHING":return R.drawable.topics16_clothing;case "BUILDINGS_PLACES":return R.drawable.topics16_buildings;
            case "SPORTS":return R.drawable.topics16_sports;case "ARTS":case "LEISURE":case "CULTURE":return R.drawable.topics16_arts;
            default:return R.drawable.topics16_book;}
    }
    static class TopicViewHolder extends RecyclerView.ViewHolder {
        TopicViewHolder(@NonNull View item){super(item);}
        TextView text(int id){return itemView.findViewById(id);}
    }
}
