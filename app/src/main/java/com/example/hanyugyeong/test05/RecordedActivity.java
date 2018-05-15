package com.example.hanyugyeong.test05;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class RecordedActivity extends AppCompatActivity {

    TextView record;
    TextFileManager textFileManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.support.v7.app.ActionBar ab = getSupportActionBar();
        ab.setTitle("Encountering Record");
        setContentView(R.layout.activity_recorded);

        textFileManager = new TextFileManager();

        record = findViewById(R.id.record);
        record.setText(textFileManager.load());
    }
}
