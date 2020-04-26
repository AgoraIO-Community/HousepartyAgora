package com.example.housepartyagora.acitivities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.housepartyagora.R;

public class SigninActivity extends AppCompatActivity {
    EditText userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        userName = findViewById(R.id.et_user_name);
    }

    public void onSignUpBackClick(View view) {
        finish();
    }

    public void onSignInNextClick(View view) {
        String name = userName.getText().toString();
        if (name != null && !name.equals("")) {
            Intent intent = new Intent(this, VideoCallActivity.class);
            intent.putExtra("userName", name);
            startActivity(intent);
        }else {
            Toast.makeText(this, "user name cannot be empty", Toast.LENGTH_SHORT).show();
        }
    }

}
