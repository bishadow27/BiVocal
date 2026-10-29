package com.example.bivoca.data.api;

import com.example.bivoca.data.model.Topic;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface TopicApi {

    @GET("topics/catalog")
    Call<List<Topic>> getTopics();
}
