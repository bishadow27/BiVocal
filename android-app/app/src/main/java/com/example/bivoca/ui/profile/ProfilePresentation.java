package com.example.bivoca.ui.profile;

import com.example.bivoca.data.model.GamificationStats;
import com.example.bivoca.data.model.LeaderboardUser;
import com.example.bivoca.data.model.LearningDay;
import com.example.bivoca.data.model.LearningHistory;
import com.example.bivoca.data.model.User;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;

/** Pure presentation of server fields. No XP, level, streak or ranking calculation. */
public final class ProfilePresentation {
    private ProfilePresentation() { }
    public static String displayName(User user) {
        if(user==null)return "";
        if(user.getFullName()!=null&&!user.getFullName().trim().isEmpty())return user.getFullName().trim();
        return user.getUsername()==null?"":user.getUsername().trim();
    }
    public static String initials(String name) {
        if(name==null||name.trim().isEmpty())return "U";
        String[] words=name.trim().split("\\s+");
        String first=words[0].substring(0,words[0].offsetByCodePoints(0,1));
        String last=words.length>1?words[words.length-1].substring(0,words[words.length-1].offsetByCodePoints(0,1)):"";
        return (first+last).toUpperCase(Locale.ROOT);
    }
    public static boolean hasLevelProgress(GamificationStats s) {
        return s!=null&&s.getLevel()>0&&s.getXpPerLevel()>0&&s.getXpIntoLevel()>=0&&s.getXpIntoLevel()<s.getXpPerLevel();
    }
    public static Integer rank(List<LeaderboardUser> rows,int userId) {
        if(rows!=null&&userId>0)for(LeaderboardUser row:rows)if(row!=null&&row.getUserId()==userId)return row.getRank()>0?row.getRank():null;
        return null;
    }
    public enum DayState { ACTIVE, COMPLETED, INACTIVE, UNKNOWN, FUTURE }
    public static final class Day {
        public final String date;public final DayState state;public final int newWords,reviews;
        private Day(String date,DayState state,int newWords,int reviews){this.date=date;this.state=state;this.newWords=newWords;this.reviews=reviews;}
    }
    private static Date parse(SimpleDateFormat format,String date) {
        if(date==null||!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))return null;
        ParsePosition position=new ParsePosition(0);Date parsed=format.parse(date,position);
        return parsed!=null&&position.getIndex()==date.length()&&format.format(parsed).equals(date)?parsed:null;
    }
    /** Dates are date-only values already supplied in the Backend's Vietnam calendar. */
    public static List<Day> week(LearningHistory history) {
        if(history==null||history.getDays().isEmpty())return Collections.emptyList();
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);format.setLenient(false);
        // UTC is used solely for date arithmetic; never convert a device timestamp into a study date.
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        TreeMap<String,LearningDay> records=new TreeMap<>();
        for(LearningDay day:history.getDays()){
            if(day==null||parse(format,day.getDate())==null||records.put(day.getDate(),day)!=null)return Collections.emptyList();
        }
        String anchor=records.lastKey();Calendar cursor=Calendar.getInstance(TimeZone.getTimeZone("UTC"),Locale.ROOT);
        Date anchorDate=parse(format,anchor);if(anchorDate==null)return Collections.emptyList();cursor.setTime(anchorDate);
        cursor.add(Calendar.DATE,-((cursor.get(Calendar.DAY_OF_WEEK)+5)%7));
        List<Day> result=new ArrayList<>(7);
        for(int i=0;i<7;i++,cursor.add(Calendar.DATE,1)){
            String date=format.format(cursor.getTime());
            if(date.compareTo(anchor)>0){result.add(new Day(date,DayState.FUTURE,0,0));continue;}
            LearningDay row=records.get(date);if(row==null)return Collections.emptyList();
            DayState state;
            if("NO_DATA".equals(row.getStatus()))state=DayState.UNKNOWN;
            else if("COMPLETED".equals(row.getStatus()))state=DayState.COMPLETED;
            else if("NOT_COMPLETED".equals(row.getStatus()))state=row.getNewWords()>0||row.getReviews()>0?DayState.ACTIVE:DayState.INACTIVE;
            else if("NO_ACTIVITY".equals(row.getStatus()))state=DayState.INACTIVE;
            else state=DayState.UNKNOWN;
            result.add(new Day(date,state,row.getNewWords(),row.getReviews()));
        }
        return Collections.unmodifiableList(result);
    }
}
