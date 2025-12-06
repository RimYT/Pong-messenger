package com.RSD.pong.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import android.app.AlertDialog;

import android.util.Base64;
import org.json.JSONObject;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

import com.RSD.pong.R;
import com.RSD.pong.RegisterData;
import com.RSD.pong.ServerResponse;
import com.RSD.pong.utils.AccountHandler;
import com.RSD.pong.utils.ApiService;
import com.RSD.pong.utils.Prefs;
import com.RSD.pong.utils.RetrofitClient;
import com.RSD.pong.utils.CheckIP;

public class RegisterActivity extends AppCompatActivity {

    EditText inputNickname, inputUsername, inputPassword, inputEmail;
    Button btnRegister, btnReInputIp;

    Prefs prefs;
    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        prefs = new Prefs(this);

        inputNickname = findViewById(R.id.inputNickname);
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        inputEmail    = findViewById(R.id.inputEmail);
        btnRegister   = findViewById(R.id.btnRegister);
        btnReInputIp  = findViewById(R.id.btnReInputIp);

        String serverIp = prefs.getServerIp();
        Retrofit retrofit = RetrofitClient.getClient(serverIp);
        api = retrofit.create(ApiService.class);

        btnRegister.setOnClickListener(v -> registerUser());
        btnReInputIp.setOnClickListener(v -> reInputIp());
    }

    private void registerUser() {

        RegisterData data = new RegisterData(
                inputUsername.getText().toString(),
                inputPassword.getText().toString(),
                inputNickname.getText().toString(),
                inputEmail.getText().toString()
        );

        api.register(data).enqueue(new Callback<ServerResponse>() {
            @Override
            public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ServerResponse body = response.body();

                    try {
                        String accessToken = body.access_token;
                        String refreshToken = body.refresh_token;

                        AccountHandler.createAccount(RegisterActivity.this, inputUsername.getText().toString(),
                                accessToken, refreshToken);

                        Toast.makeText(RegisterActivity.this, body.message, Toast.LENGTH_LONG).show();
                        goToMainApp();

                    } catch (Exception e) {
                        Toast.makeText(RegisterActivity.this,
                                "Token parsing error", Toast.LENGTH_LONG).show();
                    }

                } else {
                    try {
                        String errorBody = response.errorBody().string();
                        JSONObject json = new JSONObject(errorBody);
                        String detail = json.optString("detail", "Unknown error");
                        Toast.makeText(RegisterActivity.this, detail, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "Can't connect to server", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToMainApp() {
        // TODO: переход в основной экран приложения
        Toast.makeText(this, "Register successful!", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------
    // IP input
    // ------------------------------
    private void reInputIp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter server IP");

        final EditText input = new EditText(this);
        input.setHint("Example: 192.168.1.10");
        input.setText(prefs.getServerIp());
        builder.setView(input);

        builder.setCancelable(true);

        builder.setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String ip = input.getText().toString().trim();

            if (CheckIP.isValidIp(ip)) {
                CheckIP.pongIP(RegisterActivity.this, prefs, ip, success -> {
                    if (success) {
                        dialog.dismiss();
                    }
                });
            } else {
                Toast.makeText(RegisterActivity.this, "Invalid IP", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
