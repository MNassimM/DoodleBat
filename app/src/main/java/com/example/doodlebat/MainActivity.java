package com.example.doodlebat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startGameButton = findViewById(R.id.startGameButton);
        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GameActivity.class);
                startActivity(intent);
            }
        });
        Button viewScoresButton = findViewById(R.id.viewScoresButton);
        viewScoresButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ScoreboardActivity.class));
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Empêche l'accumulation d'activités
        if (isFinishing()) {
            Runtime.getRuntime().gc();
        }
    }
}