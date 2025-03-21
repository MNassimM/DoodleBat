package com.example.doodlebat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class GameActivity extends Activity {
    private GameView gameView;
    private RelativeLayout gameOverLayout;
    private TextView scoreText;

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
        }));    }
}