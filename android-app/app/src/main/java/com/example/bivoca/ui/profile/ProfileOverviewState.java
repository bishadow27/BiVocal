package com.example.bivoca.ui.profile;

import com.example.bivoca.data.model.*;
import java.util.List;

/** Immutable independent request states, used only by the new Profile presentation. */
public final class ProfileOverviewState {
    public static final class Section<T> {
        public final T value;public final boolean loading,complete;public final String error;
        private Section(T value,boolean loading,boolean complete,String error){this.value=value;this.loading=loading;this.complete=complete;this.error=error;}
        public static <T> Section<T> empty(){return new Section<>(null,false,false,null);}
        public static <T> Section<T> loading(Section<T> previous){return new Section<>(previous.value,true,false,null);}
        public static <T> Section<T> success(T value){return new Section<>(value,false,true,null);}
        public static <T> Section<T> failure(String error,Section<T> previous){return new Section<>(previous.value,false,true,error);}
    }
    public final Section<User> user;
    public final Section<GamificationStats> stats;
    public final Section<List<LeaderboardUser>> rank;
    public final Section<LearningHistory> week;
    public final Section<LearningGoal> goal;
    private ProfileOverviewState(Section<User> user,Section<GamificationStats> stats,Section<List<LeaderboardUser>> rank,Section<LearningHistory> week,Section<LearningGoal> goal){this.user=user;this.stats=stats;this.rank=rank;this.week=week;this.goal=goal;}
    public static ProfileOverviewState empty(){return new ProfileOverviewState(Section.empty(),Section.empty(),Section.empty(),Section.empty(),Section.empty());}
    public ProfileOverviewState loading(){return new ProfileOverviewState(Section.loading(user),Section.loading(stats),Section.loading(rank),Section.loading(week),Section.loading(goal));}
    public ProfileOverviewState withUser(Section<User> value){return new ProfileOverviewState(value,stats,rank,week,goal);}
    public ProfileOverviewState withStats(Section<GamificationStats> value){return new ProfileOverviewState(user,value,rank,week,goal);}
    public ProfileOverviewState withRank(Section<List<LeaderboardUser>> value){return new ProfileOverviewState(user,stats,value,week,goal);}
    public ProfileOverviewState withWeek(Section<LearningHistory> value){return new ProfileOverviewState(user,stats,rank,value,goal);}
    public ProfileOverviewState withGoal(Section<LearningGoal> value){return new ProfileOverviewState(user,stats,rank,week,value);}
}
