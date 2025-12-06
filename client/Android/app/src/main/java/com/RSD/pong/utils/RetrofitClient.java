package com.RSD.pong.utils;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    public static Retrofit getClient(String serverIp) {
        return new Retrofit.Builder()
                .baseUrl("http://" + serverIp + ":8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}