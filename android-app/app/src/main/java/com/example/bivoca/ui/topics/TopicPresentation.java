package com.example.bivoca.ui.topics;

import com.example.bivoca.R;

/** Shared catalog presentation; no data transformation or eligibility decisions. */
public final class TopicPresentation {
    private TopicPresentation() {}
    public static String posGlyph(String code){
        if(code==null)return "";
        switch(code){case "POS_NOUN":return "N";case "POS_VERB":return "V";case "POS_ADJECTIVE":return "Adj";case "POS_ADVERB":return "Adv";case "POS_PREPOSITION":return "Prep";case "POS_CONJUNCTION":return "Conj";case "POS_PRONOUN":return "Pron";default:return "Aa";}
    }
    public static int colorResourceFor(String code){
        if(code==null)return R.color.bivocal_light_cobalt;
        switch(code){case "FOOD":case "HEALTH":case "EMOTIONS":case "POS_PREPOSITION":return R.color.bivocal_light_coral_ink;
            case "ANIMALS":case "WEATHER":case "FINANCE":case "POS_ADVERB":return R.color.bivocal_light_honey_ink;
            case "SCHOOL":case "CULTURE":case "ARTS":case "LANGUAGE":case "POS_ADJECTIVE":case "POS_PRONOUN":return R.color.bivocal_light_lavender_ink;
            case "WORK":case "NATURE":case "SCIENCE":case "POS_VERB":return R.color.bivocal_light_mint_ink;
            case "FAMILY":case "RELATIONSHIPS":case "CLOTHING":return R.color.bivocal_light_lavender_ink;
            default:return R.color.bivocal_light_cobalt;}
    }
    public static int backgroundResourceFor(String code){int color=colorResourceFor(code);if(color==R.color.bivocal_light_coral_ink)return R.color.bivocal_light_coral_soft;if(color==R.color.bivocal_light_honey_ink)return R.color.bivocal_light_honey_soft;if(color==R.color.bivocal_light_lavender_ink)return R.color.bivocal_light_lavender_soft;if(color==R.color.bivocal_light_mint_ink)return R.color.bivocal_light_mint_soft;return R.color.bivocal_light_cobalt_soft;}
    public static int iconFor(String code){
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
}
