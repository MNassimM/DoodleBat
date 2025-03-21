package com.example.doodlebat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
    private EditText pseudoInput;

    private int tempScore;

    private Button saveScoreButton;

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
        pseudoInput = gameOverView.findViewById(R.id.pseudoInput);
        Button restartButton = gameOverView.findViewById(R.id.restartButton);
        Button mainMenuButton = gameOverView.findViewById(R.id.mainMenuButton);

        // Listener pour redémarrer
        restartButton.setOnClickListener(v -> {
            gameView.resetGame();
            gameOverLayout.setVisibility(View.GONE);
            saveScoreButton.setEnabled(true); // Réactive le bouton
        });

        // Listener pour le menu principal
        mainMenuButton.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(this, MainActivity.class));
        });

        // Définir le listener de fin de jeu
        gameView.setGameOverListener(finalScore -> runOnUiThread(() -> {
            gameOverLayout.setVisibility(View.VISIBLE);
            scoreText.setText("Score: " + finalScore);
            tempScore = finalScore;
        }));

        saveScoreButton = gameOverView.findViewById(R.id.saveScoreButton);
        saveScoreButton.setOnClickListener(v -> {
            String tempPseudo = pseudoInput.getText().toString().trim();
            if (tempPseudo.isEmpty()) {
                Toast.makeText(GameActivity.this, "Le pseudo ne peut pas être vide", Toast.LENGTH_SHORT).show();
            } else if (tempPseudo.length() > 12) {
                Toast.makeText(GameActivity.this, "Max 12 caractères", Toast.LENGTH_SHORT).show();
            } else {
                saveScore(tempPseudo, tempScore);
            }
        });
    }

    private void saveScore(String pseudo, int score) {
        SharedPreferences prefs = getSharedPreferences("Scores", MODE_PRIVATE);
        Set<String> scores = new HashSet<>(prefs.getStringSet("scores", new HashSet<>()));
        scores.add(pseudo + ":" + score);
        prefs.edit().putStringSet("scores", scores).apply();

        // Désactiver le bouton après l'enregistrement
        saveScoreButton.setEnabled(false);
    }

    // Ajoutez cette méthode pour réinitialiser l'état du bouton
    @Override
    protected void onResume() {
        super.onResume();
        if (saveScoreButton != null) {
            saveScoreButton.setEnabled(true);
        }
    }
}