package com.media.jwvideoplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.media.jwvideoplayer.ui.VodPlayActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button openPlayPage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        openPlayPage = findViewById(R.id.open_play_page);

        openPlayPage.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.open_play_page) {
            startActivity(new Intent(this, VodPlayActivity.class));
        }
    }
}