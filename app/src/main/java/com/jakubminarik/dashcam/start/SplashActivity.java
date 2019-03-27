package com.jakubminarik.dashcam.start;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.record.RecordActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppThemeNoActionBar);
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, RecordActivity.class));
        finish();
    }
}
