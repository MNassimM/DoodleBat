package com.example.doodlebat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ScoreboardActivity extends Activity {
    private ArrayList<ScoreEntry> scoreEntries = new ArrayList<>();
    private MediaPlayer scoreboardMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoreboard);

        scoreboardMusic = MediaPlayer.create(this, R.raw.revenge);
        scoreboardMusic.setLooping(true);
        scoreboardMusic.start();


        ListView scoreListView = findViewById(R.id.scoreListView);
        Button backButton = findViewById(R.id.backButton);

        SharedPreferences prefs = getSharedPreferences("Scores", MODE_PRIVATE);
        Set<String> scoresSet = prefs.getStringSet("scores", new HashSet<>());

        for(String entry : scoresSet) {
            String[] parts = entry.split(":");
            if(parts.length == 2) {
                scoreEntries.add(new ScoreEntry(parts[0], Integer.parseInt(parts[1])));
            }
        }

        scoreEntries.sort((o1, o2) -> {
            // Tri principal par score
            int scoreCompare = Integer.compare(o2.score, o1.score);
            if (scoreCompare != 0) return scoreCompare;

            // Tri secondaire par pseudo
            return o1.pseudo.compareToIgnoreCase(o2.pseudo);
        });

        ArrayAdapter<ScoreEntry> adapter = new ArrayAdapter<ScoreEntry>(
                this,
                R.layout.score_item, // Layout personnalisé
                scoreEntries
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.score_item, parent, false);
                }

                ScoreEntry entry = getItem(position);
                TextView pseudoText = convertView.findViewById(R.id.pseudoText);
                TextView scoreText = convertView.findViewById(R.id.scoreText);

                pseudoText.setText(entry.pseudo);
                scoreText.setText(String.valueOf(entry.score));

                return convertView;
            }
        };

        scoreListView.setAdapter(adapter);
        backButton.setOnClickListener(v -> finish());
    }
    private static class ScoreEntry {
        String pseudo;
        int score;

        ScoreEntry(String pseudo, int score) {
            this.pseudo = pseudo;
            this.score = score;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(scoreboardMusic != null && scoreboardMusic.isPlaying()) {
            scoreboardMusic.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(scoreboardMusic != null && !scoreboardMusic.isPlaying()) {
            scoreboardMusic.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(scoreboardMusic != null) {
            scoreboardMusic.stop();
            scoreboardMusic.release();
            scoreboardMusic = null;
        }
    }
}