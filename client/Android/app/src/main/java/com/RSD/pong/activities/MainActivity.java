package com.RSD.pong.activities;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.RSD.pong.models.LoginServerResponse;
import com.RSD.pong.R;
import com.RSD.pong.utils.AccountHandler;
import com.RSD.pong.utils.ApiService;
import com.RSD.pong.utils.CheckIP;
import com.RSD.pong.utils.Prefs;
import com.RSD.pong.utils.RetrofitClient;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    Prefs prefs;

    Button btnReconnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //asking for get_accounts permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    101);
        }

        btnReconnect = findViewById(R.id.btnReconnect);
        prefs = new Prefs(this);

        //if we dont have ip - show ip enter dialog
        if (prefs.getServerIp() == null) {
            showIpDialog();
        } else {
            checkSavedAccount();
        }

        btnReconnect.setOnClickListener(v -> checkSavedAccount());
    }

    private void showIpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter server IP");

        final EditText input = new EditText(this);
        input.setHint("Example: 192.168.1.10");
        input.setText(prefs.getServerIp());
        builder.setView(input);

        builder.setCancelable(false);

        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String ip = input.getText().toString().trim();

            if (CheckIP.isValidIp(ip)) {
                CheckIP.pongIP(this, prefs, ip, success -> {
                    if (success) {
                        dialog.dismiss();
                        checkSavedAccount();
                    }
                });
            } else {
                Toast.makeText(this, "Invalid IP", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkSavedAccount() {
        //getting saved account
        Account saved = AccountHandler.getSavedAccount(this);

        //if we dont have saved account - open register screen
        if (saved == null) {
            openRegisterScreen();
            return;
        }

        AccountManager am = AccountManager.get(this);

        //getting account data
        String access = am.peekAuthToken(saved, "access");
        String refresh = am.getUserData(saved, "refresh");
        String savedIp = am.getUserData(saved, "server_ip");

        //checking ip (cuz user could change it)
        if (savedIp == null || !savedIp.equals(prefs.getServerIp())) {
            openRegisterScreen();
            return;
        }

        ApiService api = RetrofitClient.getClient(prefs.getServerIp()).create(ApiService.class);

        //sending login by token request to server
        api.loginByToken(access, refresh).enqueue(new Callback<LoginServerResponse>() {
            @Override
            public void onResponse(Call<LoginServerResponse> call, Response<LoginServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    //if server said that all ok - go to main app
                    goToMainApp();
                } else {
                    try {
                        //printing error message
                        String errorBody = response.errorBody().string();
                        JSONObject json = new JSONObject(errorBody);
                        String detail = json.optString("detail", "Unknown error");
                        Toast.makeText(MainActivity.this, detail, Toast.LENGTH_LONG).show();

                        //401 - unauthorized
                        if (response.code() == 401) {
                            AccountHandler.deleteAccount(MainActivity.this, saved);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    openRegisterScreen();
                }
            }

            @Override
            public void onFailure(Call<LoginServerResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Can't connect to server", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMainApp() {
        startActivity(new Intent(this, AppActivity.class));
        finish();
    }

    private void openRegisterScreen() {
        startActivity(new Intent(this, RegisterActivity.class));
        finish();
    }
}
