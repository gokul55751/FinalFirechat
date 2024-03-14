package com.example.finalfirechat;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.example.finalfirechat.databinding.ActivitySplashScreenBinding;

public class SplashScreenActivity extends AppCompatActivity {

ActivitySplashScreenBinding binding;
    private String TAG = "log9999";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences preferences = getSharedPreferences("application", MODE_PRIVATE);
        String selfUuid = preferences.getString("self_uuid", null);

        new Handler().postDelayed(() -> {
            if(selfUuid!=null){
                Log.d(TAG, "onCreate: " + selfUuid);
                startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
                finish();
            }else{
                startActivity(new Intent(SplashScreenActivity.this, CreateProfileActivity.class));
                finish();
            }
        }, 2500);
    }
}