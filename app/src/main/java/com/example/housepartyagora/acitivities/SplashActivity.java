package com.example.housepartyagora.acitivities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.housepartyagora.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    public void onLoginButtonClick(View view) {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
    }

    public void onSignUpButtonClick(View view) {
        Intent intent = new Intent(SplashActivity.this, SigninActivity.class);
        startActivity(intent);
    }
}
