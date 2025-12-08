package com.RSD.pong.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.util.Base64;
import android.util.Log;

import com.RSD.pong.models.ServerResponse;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class AccountHandler {
    static Prefs prefs;
    public static void createAccount(Activity activity, String username, String accessToken, String refreshToken) {
        prefs = new Prefs(activity);
        AccountManager accountManager = AccountManager.get(activity);
        Account account = new Account(username, "com.RSD.pong.account");

        Account existing = getSavedAccount(activity);
        if (existing != null) deleteAccount(activity, existing);

        //saving some amount of data
        accountManager.addAccountExplicitly(account, null, null);
        accountManager.setAuthToken(account, "access", accessToken);
        accountManager.setUserData(account, "refresh", refreshToken);
        accountManager.setUserData(account, "server_ip", prefs.getServerIp());

        //generating pair of keys
        KeyPair keyPair = generateRSAKeyPair();
        if (keyPair != null) {
            savePrivateKey(activity, account, keyPair.getPrivate());
            sendPublicKeyToServer(username, keyPair.getPublic(), prefs.getServerIp());
        }
    }
    //utility function (for generating pair of keys, who would think about it??)
    private static KeyPair generateRSAKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            return keyGen.generateKeyPair();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    //saving private key
    private static void savePrivateKey(Activity activity, Account account, PrivateKey privateKey) {
        String privateKeyStr = Base64.encodeToString(privateKey.getEncoded(), Base64.DEFAULT);
        AccountManager accountManager = AccountManager.get(activity);
        accountManager.setUserData(account, "private_key", privateKeyStr);
    }
    //sending public key to server (isnt that obvious??)
    private static void sendPublicKeyToServer(String username, PublicKey publicKey, String serverIp) {
        String publicKeyStr = Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT);
        Retrofit retrofit = RetrofitClient.getClient(serverIp);
        ApiService api = retrofit.create(ApiService.class);

        api.addPublicKey(username, publicKeyStr).enqueue(new Callback<ServerResponse>() {
            @Override
            public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("PublicKey", "Sent successfully");
                } else {
                    Log.e("PublicKey", "Error sending key");
                }
            }

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {
                Log.e("PublicKey", "Failed to connect to server");
            }
        });
    }

    public static Account getSavedAccount(Activity context) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType("com.RSD.pong.account");
        return accounts.length > 0 ? accounts[0] : null;
    }

    public static void deleteAccount(Activity activity, Account account) {
        AccountManager am = AccountManager.get(activity);

        if (account == null) {
            return;
        }

        boolean removed = am.removeAccountExplicitly(account);
    }
}
