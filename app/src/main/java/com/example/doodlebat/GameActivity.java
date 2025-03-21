package com.example.doodlebat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public class GameActivity extends Activity {
    private GameView gameView;
    private RelativeLayout gameOverLayout;
    private TextView scoreText;

    private int tempScore;
    private String tempPseudo;

    private Button saveScoreButton;

    private MediaPlayer gameMusic;
    private MediaPlayer gameOverMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Utilisez un FrameLayout pour superposer les éléments
        FrameLayout rootLayout = new FrameLayout(this);
        gameView = new GameView(this);
        rootLayout.addView(gameView);

        // Ajoutez le layout de Game Over
        View gameOverView = getLayoutInflater().inflate(R.layout.game_over, rootLayout, false);
        rootLayout.addView(gameOverView);

        setContentView(rootLayout);

        gameOverLayout = gameOverView.findViewById(R.id.gameOverLayout);
        scoreText = gameOverView.findViewById(R.id.scoreText);
        Button restartButton = gameOverView.findViewById(R.id.restartButton);
        Button mainMenuButton = gameOverView.findViewById(R.id.mainMenuButton);

        // Listener pour redémarrer
        restartButton.setOnClickListener(v -> {
            gameView.resetGame();
            gameOverLayout.setVisibility(View.GONE);
            saveScoreButton.setEnabled(true); // Réactive le bouton

            stopGameOverMusic();
            startGameMusic();
        });

        // Listener pour le menu principal
        mainMenuButton.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(this, MainActivity.class));
        });

        gameMusic = MediaPlayer.create(this, R.raw.new_chap);
        gameMusic.setLooping(true);
        gameMusic.start();


        // Définir le listener de fin de jeu
        gameView.setGameOverListener(finalScore -> runOnUiThread(() -> {
            gameOverLayout.setVisibility(View.VISIBLE);
            scoreText.setText("Score: " + finalScore);
            tempScore=finalScore;
            stopGameMusic();
            gameOverMusic = MediaPlayer.create(this, R.raw.toothpaste);
            gameOverMusic.setLooping(true);
            gameOverMusic.start();
        }));

        saveScoreButton = gameOverView.findViewById(R.id.saveScoreButton);
        saveScoreButton.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
            builder.setTitle("Enregistrer le score");
            final EditText input = new EditText(GameActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("Entrez votre pseudo");
            builder.setView(input);

            // Dans le dialog
            builder.setPositiveButton("OK", (dialog, which) -> {
                tempPseudo = input.getText().toString().trim();
                if(tempPseudo.isEmpty()) {
                    Toast.makeText(GameActivity.this, "Le pseudo ne peut pas être vide", Toast.LENGTH_SHORT).show();
                } else if(tempPseudo.length() > 12) {
                    Toast.makeText(GameActivity.this, "Max 12 caractères", Toast.LENGTH_SHORT).show();
                } else {
                    saveScore(tempPseudo, tempScore);
                }
            });
            builder.setNegativeButton("Annuler", null);
            builder.show();

        });
    }

    private void stopGameMusic() {
        if (gameMusic != null) {
            gameMusic.stop();
            gameMusic.release();
            gameMusic = null;
        }
    }

    private void stopGameOverMusic() {
        if (gameOverMusic != null) {
            gameOverMusic.stop();
            gameOverMusic.release();
            gameOverMusic = null;
        }
    }

    private void startGameMusic() {
        stopGameMusic(); // Arrête et nettoie toute instance existante
        gameMusic = MediaPlayer.create(this, R.raw.new_chap);
        gameMusic.setLooping(true);
        gameMusic.start();
    }

    private void saveScore(String pseudo, int score) {
        SharedPreferences prefs = getSharedPreferences("Scores", MODE_PRIVATE);
        Set<String> scores = new HashSet<>(prefs.getStringSet("scores", new HashSet<>()));
        scores.add(pseudo + ":" + score);
        prefs.edit().putStringSet("scores", scores).apply();

        // Désactiver le bouton après l'enregistrement
        saveScoreButton.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopGameMusic();
        stopGameOverMusic();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if(gameMusic != null && gameMusic.isPlaying()) {
            gameMusic.pause();
        }
    }

    // Ajoutez cette méthode pour réinitialiser l'état du bouton
    @Override
    protected void onResume() {
        super.onResume();
        if(saveScoreButton != null) {
            saveScoreButton.setEnabled(true);
        }
        if(gameMusic != null && !gameMusic.isPlaying()) {
            gameMusic.start();
        }
    }
}