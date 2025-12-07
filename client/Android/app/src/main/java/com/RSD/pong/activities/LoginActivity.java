package com.RSD.pong.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import android.app.AlertDialog;

import org.json.JSONObject;

import com.RSD.pong.LoginServerResponse;
import com.RSD.pong.R;
import com.RSD.pong.utils.AccountHandler;
import com.RSD.pong.utils.ApiService;
import com.RSD.pong.utils.Prefs;
import com.RSD.pong.utils.RetrofitClient;
import com.RSD.pong.utils.CheckIP;

public class LoginActivity extends AppCompatActivity {

    EditText inputUsername, inputPassword;
    Button btnLogin, btnReInputIp;
    TextView regText;
    Prefs prefs;
    ApiService api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = new Prefs(this);

        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin     = findViewById(R.id.btnLogin);
        btnReInputIp = findViewById(R.id.btnReInputIp);

        regText = findViewById(R.id.regText);

        String serverIp = prefs.getServerIp();
        Retrofit retrofit = RetrofitClient.getClient(serverIp);
        api = retrofit.create(ApiService.class);

        btnLogin.setOnClickListener(v -> loginUser());
        btnReInputIp.setOnClickListener(v -> reInputIp());

        // creating link to RegisterActivity to reg text
        String txt = regText.getText().toString();
        SpannableString span = new SpannableString(txt);
        span.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
                finish();
            }
        }, txt.length() - 9, txt.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        regText.setText(span);
        regText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void loginUser() {

        String username = inputUsername.getText().toString();
        String password = inputPassword.getText().toString();

        api.login(username, password).enqueue(new Callback<LoginServerResponse>() {
            @Override
            public void onResponse(Call<LoginServerResponse> call, Response<LoginServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginServerResponse body = response.body();

                    try {
                        //getting tokens
                        String access = body.access_token;
                        String refresh = body.refresh_token;

                        //creating account
                        AccountHandler.createAccount(
                                LoginActivity.this,
                                username,
                                access,
                                refresh
                        );

                        Toast.makeText(LoginActivity.this, body.message, Toast.LENGTH_LONG).show();
                        goToMainApp();

                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this,
                                "Token parsing error", Toast.LENGTH_LONG).show();
                    }

                } else {
                    try {
                        //printing error message
                        String err = response.errorBody().string();
                        JSONObject json = new JSONObject(err);
                        String detail = json.optString("detail", "Unknown error");
                        Toast.makeText(LoginActivity.this, detail, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginServerResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this,
                        "Can't connect to server", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void goToMainApp() {
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        // TODO: переход в основной экран
    }

    //IP input
    private void reInputIp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter server IP");

        final EditText input = new EditText(this);
        input.setHint("Example: 192.168.1.10");
        input.setText(prefs.getServerIp());
        builder.setView(input);

        builder.setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String ip = input.getText().toString().trim();

            if (CheckIP.isValidIp(ip)) {
                CheckIP.pongIP(LoginActivity.this, prefs, ip, success -> {
                    if (success) dialog.dismiss();
                });
            } else {
                Toast.makeText(LoginActivity.this, "Invalid IP", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
