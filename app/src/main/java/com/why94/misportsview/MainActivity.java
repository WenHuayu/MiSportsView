package com.why94.misportsview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private MiSportsView mMiSportsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMiSportsView = (MiSportsView) findViewById(R.id.v_mi_sports);
    }

    public void connect(View view) {
        mMiSportsView.connect();
    }

    public void connected(View view) {
        mMiSportsView.connected();
    }
}
