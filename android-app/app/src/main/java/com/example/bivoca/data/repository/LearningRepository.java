package com.example.bivoca.data.repository;
import android.content.Context; import androidx.annotation.NonNull;
import com.example.bivoca.data.api.*; import com.example.bivoca.data.model.*;
import java.util.List; import retrofit2.*;
public final class LearningRepository {
 private final LearningApi api;
 public LearningRepository(Context context){api=AuthenticatedRetrofitClient.getInstance(context).create(LearningApi.class);}
 public interface Result<T>{void onSuccess(T value);void onError(String message);}
 private <T> Call<T> enqueue(Call<T> call,String label,Result<T> result){call.enqueue(new Callback<T>(){
  @Override public void onResponse(@NonNull Call<T> c,@NonNull Response<T> r){if(c.isCanceled())return;if(r.isSuccessful()&&r.body()!=null)result.onSuccess(r.body());else{if(r.errorBody()!=null)r.errorBody().close();result.onError("Không "+label+" (HTTP "+r.code()+").");}}
  @Override public void onFailure(@NonNull Call<T> c,@NonNull Throwable t){if(!c.isCanceled())result.onError("Không kết nối được máy chủ.");}});return call;}
 public Call<LearningGoal> getGoal(Result<LearningGoal> r){return enqueue(api.getGoal(),"tải được mục tiêu",r);}
 public Call<LearningGoal> updateGoal(int goal,Result<LearningGoal> r){return enqueue(api.updateGoal(new UpdateLearningGoalRequest(goal)),"lưu được mục tiêu",r);}
 public Call<LearningToday> getToday(Result<LearningToday> r){return enqueue(api.getToday(),"tải được tiến độ hôm nay",r);}
 public Call<LearningHistory> getHistory(Result<LearningHistory> r){return enqueue(api.getHistory(7),"tải được lịch sử 7 ngày",r);}
 public Call<List<Flashcard>> getNewCards(int limit,Result<List<Flashcard>> r){return enqueue(api.getNewCards(limit),"tải được từ mới",r);}
}
