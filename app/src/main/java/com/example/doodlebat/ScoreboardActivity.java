package com.example.doodlebat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ScoreboardActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoreboard);

        ListView scoreListView = findViewById(R.id.scoreListView);
        Button backButton = findViewById(R.id.backButton);

        // Récupérer les scores
        SharedPreferences prefs = getSharedPreferences("Scores", MODE_PRIVATE);
        Set<String> scoresSet = prefs.getStringSet("scores", new HashSet<>());
        ArrayList<String> scores = new ArrayList<>(scoresSet);

        // Trier par ordre décroissant
        scores.sort(Collections.reverseOrder());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                scores
        );
        scoreListView.setAdapter(adapter);

        backButton.setOnClickListener(v -> finish());
    }
}