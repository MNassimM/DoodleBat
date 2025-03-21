package com.example.doodlebat;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
    private RelativeLayout pauseMenuLayout; // Le layout de pause
    private TextView scoreText;
    private EditText pseudoInput;
    private int tempScore;
    private String tempPseudo;
    private Button saveScoreButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        FrameLayout rootLayout = new FrameLayout(this);
        gameView = new GameView(this);
        rootLayout.addView(gameView);

        View gameOverView = getLayoutInflater().inflate(R.layout.game_over, rootLayout, false);
        rootLayout.addView(gameOverView);
        gameOverLayout = gameOverView.findViewById(R.id.gameOverLayout);
        scoreText = gameOverView.findViewById(R.id.scoreText);
        pseudoInput = gameOverView.findViewById(R.id.pseudoInput);
        Button restartButton = gameOverView.findViewById(R.id.restartButton);
        Button mainMenuButton = gameOverView.findViewById(R.id.mainMenuButton);
        saveScoreButton = gameOverView.findViewById(R.id.saveScoreButton);

        // Listener pour redémarrer
        restartButton.setOnClickListener(v -> {
            gameView.resetGame();
            gameOverLayout.setVisibility(View.GONE);
            saveScoreButton.setEnabled(true);
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

        saveScoreButton.setOnClickListener(v -> {
            String tempPseudo = pseudoInput.getText().toString().trim();
            if (tempPseudo.isEmpty()) {
                Toast.makeText(GameActivity.this, "Le pseudo ne peut pas être vide", Toast.LENGTH_SHORT).show();
            } else if (tempPseudo.length() > 12) {
                Toast.makeText(GameActivity.this, "Max 12 caractères", Toast.LENGTH_SHORT).show();
            } else {
                saveScore(tempPseudo, tempScore);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
            builder.setTitle("Enregistrer le score");
            final EditText input = new EditText(GameActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setHint("Entrez votre pseudo");
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                tempPseudo = input.getText().toString().trim();
                if (tempPseudo.isEmpty()) {
                    Toast.makeText(GameActivity.this, "Le pseudo ne peut pas être vide", Toast.LENGTH_SHORT).show();
                } else if (tempPseudo.length() > 12) {
                    Toast.makeText(GameActivity.this, "Max 12 caractères", Toast.LENGTH_SHORT).show();
                } else {
                    saveScore(tempPseudo, tempScore);
                }
            });
            builder.setNegativeButton("Annuler", null);
            builder.show();
        });

        pauseMenuLayout = (RelativeLayout) getLayoutInflater().inflate(R.layout.pause_menu, rootLayout, false);
        rootLayout.addView(pauseMenuLayout);

        Button pauseButton = new Button(this);
        pauseButton.setText("Pause");
        FrameLayout.LayoutParams pauseButtonParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        pauseButtonParams.leftMargin = 20;
        pauseButtonParams.topMargin = 20;
        pauseButton.setLayoutParams(pauseButtonParams);
        rootLayout.addView(pauseButton);

        pauseButton.setOnClickListener(v -> {
            gameView.pauseGame();
            pauseMenuLayout.setVisibility(View.VISIBLE);
        });

        Button resumeButton = pauseMenuLayout.findViewById(R.id.resumeButton);
        Button quitButton = pauseMenuLayout.findViewById(R.id.quitButton);

        resumeButton.setOnClickListener(v -> {
            pauseMenuLayout.setVisibility(View.GONE);
            gameView.resumeGame();
        });

        quitButton.setOnClickListener(v -> {
            finish();
            startActivity(new Intent(this, MainActivity.class));
        });

        setContentView(rootLayout);
    }

    private void saveScore(String pseudo, int score) {
        SharedPreferences prefs = getSharedPreferences("Scores", MODE_PRIVATE);
        Set<String> scores = new HashSet<>(prefs.getStringSet("scores", new HashSet<>()));
        scores.add(pseudo + ":" + score);
        prefs.edit().putStringSet("scores", scores).apply();
        saveScoreButton.setEnabled(false);
        saveScoreButton.setVisibility(View.GONE);
        pseudoInput.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (saveScoreButton != null) {
            saveScoreButton.setEnabled(true);
        }
    }
}