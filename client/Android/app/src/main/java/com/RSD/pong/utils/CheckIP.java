package com.RSD.pong.utils;

import android.content.Context;
import android.widget.Toast;

import com.RSD.pong.ServerResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.util.function.Consumer;

public class CheckIP {
    static ApiService api;
    static Prefs prefs;

    public static void pongIP(Context context, Prefs prefs, String ip, Consumer<Boolean> callback) {
        Retrofit retrofit = RetrofitClient.getClient(ip);
        api = retrofit.create(ApiService.class);

        Toast.makeText(context, "Pinging server...", Toast.LENGTH_SHORT).show();

        api.ping_pong().enqueue(new Callback<ServerResponse>() {
            @Override
            public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {

                if (response.isSuccessful() && response.body() != null) {
                    String status = response.body().status;
                    String message = response.body().message;

                    if ("ok".equals(status) && "pong-ping!".equals(message)) {

                        prefs.setServerIp(ip);
                        Toast.makeText(context, "Pong! IP updated", Toast.LENGTH_SHORT).show();

                        callback.accept(true);   // ← возвращаем true
                        return;
                    }
                }

                Toast.makeText(context,
                        "Looks like this server doesn't support Pong!",
                        Toast.LENGTH_SHORT).show();

                callback.accept(false);          // ← возвращаем false
            }

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {

                Toast.makeText(context,
                        "Can't connect to server",
                        Toast.LENGTH_LONG).show();

                callback.accept(false);
            }
        });
    }

    public static boolean isValidIp(String ip) {
        return ip.matches("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    }
}

