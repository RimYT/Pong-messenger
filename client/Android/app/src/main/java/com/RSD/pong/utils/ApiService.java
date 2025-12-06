package com.RSD.pong.utils;

import com.RSD.pong.LoginServerResponse;
import com.RSD.pong.RegisterData;
import com.RSD.pong.ServerResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {

    @POST("register")
    Call<ServerResponse> register(@Body RegisterData data);

    @POST("ping-pong")
    Call<ServerResponse> ping_pong();

    @FormUrlEncoded
    @POST("login_by_token")
    Call<LoginServerResponse> loginByToken(@Field("access_token") String access, @Field("refresh_token") String refresh);
}