package com.example.doodlebat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {
    private GameThread thread;
    private SensorManager sensorManager;
    private Sensor accelerometer, proximitySensor, lightSensor;
    private float batX = 400, batY = 600;
    private boolean isDark = false;
    private int screenWidth, screenHeight;
    private float lastTouchY;
    private ArrayList<Obstacle> obstacles;
    private Random random;
    private float sonarRadius1 = 0;
    private float sonarRadius2 = 200;
    private long lastUpdateTime = System.currentTimeMillis();

    private int desiredWidth = 130;
    private int desiredHeight = 130;

    private Bitmap[] batFrames = new Bitmap[2];
    private int currentFrameIndex = 0;
    private int frameCounter = 0;
    private static final int FRAME_DELAY = 10; // Ajustez pour la vitesse
    private Bitmap background, darkBackground, stalactiteBitmapTop, stalactiteBitmapBottom;
    private float backgroundX = 0; // Position X pour le défilement
    private int backgroundSpeed = 5; // Vitesse de défilement

    private Obstacle lastObstacle1 = null;
    private Obstacle lastObstacle2 = null;
    private int score = 0;
    private long lastScoreUpdateTime = System.currentTimeMillis();
    private boolean gameOver = false;
    private GameOverListener gameOverListener;
    private ArrayList<Coin> coins;
    private long lastCoinSpawnTime = System.currentTimeMillis();
    private Bitmap coinImage;
    private int lives = 3;
    private Bitmap heartImage;
    private boolean isInvincible = false;
    private long invincibilityStartTime;
    private static final long INVINCIBILITY_DURATION = 3000; // 3 secondes



    public interface GameOverListener {
        void onGameOver(int finalscore);
    }

    private Bitmap scaledBackground = null;

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }


    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        Bitmap original1 = BitmapFactory.decodeResource(getResources(), R.drawable.bat_frame_1);
        Bitmap original2 = BitmapFactory.decodeResource(getResources(), R.drawable.bat_frame_2);
        heartImage = BitmapFactory.decodeResource(getResources(), R.drawable.heart);

        batFrames[0] = Bitmap.createScaledBitmap(original1, desiredWidth, desiredHeight, true);
        batFrames[1] = Bitmap.createScaledBitmap(original2, desiredWidth, desiredHeight, true);
        heartImage = Bitmap.createScaledBitmap(heartImage, 60, 60, true);

        background = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        coinImage = BitmapFactory.decodeResource(getResources(), R.drawable.coin);

        if (background == null) {
            throw new RuntimeException("Background image not found!");
        }
        Bitmap stalactiteOriginal = BitmapFactory.decodeResource(getResources(), R.drawable.stalactite);
        stalactiteBitmapTop = Bitmap.createScaledBitmap(stalactiteOriginal, 100, 800, true);
        stalactiteBitmapBottom = Bitmap.createScaledBitmap(stalactiteBitmapTop, stalactiteBitmapTop.getWidth(), -stalactiteBitmapTop.getHeight(), true);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        thread = new GameThread(getHolder(), this);

        obstacles = new ArrayList<>();
        random = new Random();

        coins = new ArrayList<>();
        coinImage = Bitmap.createScaledBitmap(coinImage, 80, 80, true);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
        screenWidth = getWidth();
        screenHeight = getHeight();
        scaledBackground = Bitmap.createScaledBitmap(background, screenWidth, screenHeight, true);
        batX = screenWidth / 2f;
        batY = screenHeight / 2f;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        while (retry) {
            try {
                thread.setRunning(false);
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas != null) {
            Paint scorePaint = new Paint();
            scorePaint.setColor(Color.WHITE);
            scorePaint.setTextSize(50);
            canvas.drawText("Score: " + score, 20, 50, scorePaint);
            if (gameOver) {
                Paint overlayPaint = new Paint();
                overlayPaint.setColor(Color.argb(150, 0, 0, 0));
                canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);
            }
            if (isDark) {
                canvas.drawBitmap(darkBackground, backgroundX, 0, null);
                canvas.drawBitmap(darkBackground, backgroundX + screenWidth, 0, null);
            } else {
                canvas.drawBitmap(scaledBackground, backgroundX, 0, null);
                canvas.drawBitmap(scaledBackground, backgroundX + screenWidth, 0, null);
            }

            if (!gameOver) {
                backgroundX -= backgroundSpeed;
                if (backgroundX <= -screenWidth) {
                    backgroundX = 0;
                }
            }

            Bitmap currentBitmap = batFrames[currentFrameIndex];
            if (currentBitmap != null) {
                canvas.drawBitmap(
                        currentBitmap,
                        batX - (float) currentBitmap.getWidth() / 2,
                        batY - (float) currentBitmap.getHeight() / 2,
                        null
                );
            }
            Paint paint = new Paint();

            if (isDark) {
                drawSonarWaves(canvas, paint);
            }

            paint.setColor(Color.WHITE);

            paint.setTextSize(65);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            String scoreText = "Score: " + score;
            float textWidth = paint.measureText(scoreText);
            float xPosition = (screenWidth - textWidth) / 2;
            float yPosition = 150;

            canvas.drawText(scoreText, xPosition, yPosition, paint);

            for (Coin coin : coins) {
                canvas.drawBitmap(coinImage, coin.x - coin.radius, coin.y - coin.radius, null);
            }

            for (int i = 0; i < lives; i++) {
                canvas.drawBitmap(heartImage, screenWidth - (i + 1) * 65, 20, null);
            }

            if (!isInvincible || (System.currentTimeMillis() / 200) % 2 == 0) {
                canvas.drawBitmap(
                        batFrames[currentFrameIndex],
                        batX - (float) batFrames[currentFrameIndex].getWidth() / 2,
                        batY - (float) batFrames[currentFrameIndex].getHeight() / 2,
                        null
                );
            }
        }
    }


    private void drawSonarWaves(Canvas canvas, Paint paint) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        canvas.drawCircle(batX, batY, sonarRadius1, paint);
        canvas.drawCircle(batX, batY, sonarRadius2, paint);

        for (Obstacle obstacle : obstacles) {
            if (isObstacleTouchedByWave(obstacle, sonarRadius1) || isObstacleTouchedByWave(obstacle, sonarRadius2)) {
                canvas.drawBitmap(obstacle.bitmap, obstacle.x-50, obstacle.y, paint);
            }

            // Draw hitbox for debugging

            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            canvas.drawRect(obstacle.x, obstacle.y, obstacle.x + obstacle.width, obstacle.y + obstacle.height, paint);

        }
    }

    private boolean isObstacleTouchedByWave(Obstacle obstacle, float sonarRadius) {
        float closestX = Math.max(obstacle.x, Math.min(batX, obstacle.x + obstacle.width));
        float closestY = Math.max(obstacle.y, Math.min(batY, obstacle.y + obstacle.height));
        float distanceX = batX - closestX;
        float distanceY = batY - closestY;
        return (distanceX * distanceX + distanceY * distanceY) <= (sonarRadius * sonarRadius);
    }

    private void spawnCoins() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastCoinSpawnTime >= 5000) {
            float coinX = screenWidth - 150;
            float coinY = random.nextInt(screenHeight - 100) + 50;

            boolean isOverlapping = false;
            for (Obstacle obstacle : obstacles) {
                if (coinX + 50 > obstacle.x && coinX < obstacle.x + obstacle.width &&
                        coinY + 50 > obstacle.y && coinY < obstacle.y + obstacle.height) {
                    isOverlapping = true;
                    break;
                }
            }

            if (!isOverlapping) {
                coins.add(new Coin(coinX, coinY, 30));
            }

            lastCoinSpawnTime = currentTime;
        }
    }



    public void update() {

        if (gameOver) return;
        frameCounter++;
        if (frameCounter >= FRAME_DELAY) {
            currentFrameIndex = (currentFrameIndex + 1) % 2;
            frameCounter = 0;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScoreUpdateTime >= 2000) {
            score++;
            lastScoreUpdateTime = currentTime;
        }

        if (random.nextInt(100) < 5) {
            int height = 440 + random.nextInt(201); // Height between 440 and 640
            int y = random.nextBoolean() ? 0 : (screenHeight - height);

          if (lastObstacle1 != null && lastObstacle2 != null && lastObstacle1.y == lastObstacle2.y) {
                y = lastObstacle1.y == 0 ? (screenHeight - height) : 0;
            }
            int minDistance = 200;

            if (obstacles.isEmpty() || (screenWidth - obstacles.get(obstacles.size() - 1).x) > minDistance) {
                Obstacle newObstacle = new Obstacle(screenWidth, y, 100, height, y == 0 ? stalactiteBitmapTop : stalactiteBitmapBottom);
                obstacles.add(newObstacle);
                lastObstacle2 = lastObstacle1;
                lastObstacle1 = newObstacle;
            }
        }

        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            obstacle.x -= 5;

            if (obstacle.x + obstacle.width < 0) {
                iterator.remove();
            }

            if (batX + 50 > obstacle.x && batX - 50 < obstacle.x + obstacle.width &&
                    batY + 50 > obstacle.y && batY - 50 < obstacle.y + obstacle.height && !isInvincible) {
                lives--;
                if (lives <= 0) {
                    gameOver = true;
                    if (gameOverListener != null) gameOverListener.onGameOver(score);
                } else {
                    isInvincible = true;
                    invincibilityStartTime = System.currentTimeMillis();
                }
            }
        }

        if (isInvincible) {
            long currentTime2 = System.currentTimeMillis();
            if (currentTime2 - invincibilityStartTime >= INVINCIBILITY_DURATION) {
                isInvincible = false;
            }
        }
        if (currentTime - lastUpdateTime > 50) {
            sonarRadius1 += 10;
            sonarRadius2 += 10;
            lastUpdateTime = currentTime;
        }

        if (sonarRadius1 > screenWidth/2) {
            sonarRadius1 = 0;
        }
        if (sonarRadius2 > screenWidth/2) {
            sonarRadius2 = 0;
        }

        spawnCoins();

        Iterator<Coin> coinIterator = coins.iterator();
        while (coinIterator.hasNext()) {
            Coin coin = coinIterator.next();
            coin.x -= 5;

            if (coin.x + coin.radius < 0) {
                coinIterator.remove();
            }

            if (Math.hypot(coin.x - batX, coin.y - batY) <= coin.radius + 50) {
                score += 10;
                coinIterator.remove();
            }
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (gameOver) return;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            batX -= event.values[0] * 5;
            if (batX < 0) batX = 0;
            if (batX > screenWidth) batX = screenWidth;
        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            boolean wasDark = isDark;
            isDark = event.values[0] < 5;
            if (isDark && !wasDark) {
                // Vérifier si scaledBackground est initialisé
                if (scaledBackground != null) {
                    darkBackground = Bitmap.createBitmap(
                            scaledBackground.getWidth(),
                            scaledBackground.getHeight(),
                            scaledBackground.getConfig()
                    );
                    Canvas canvas = new Canvas(darkBackground);
                    Paint darkPaint = new Paint();
                    ColorMatrix colorMatrix = new ColorMatrix();
                    colorMatrix.setScale(0.5f, 0.5f, 0.5f, 1.0f);
                    ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
                    darkPaint.setColorFilter(filter);
                    canvas.drawBitmap(scaledBackground, 0, 0, darkPaint);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOver) return true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getY() - lastTouchY;
                batY += deltaY;
                lastTouchY = event.getY();
                if (batY < 0) batY = 0;
                if (batY > screenHeight) batY = screenHeight;
                break;
        }
        return true;
    }

    public void resetGame() {
        score = 0;
        gameOver = false;
        obstacles.clear();
        sonarRadius1 = 0;
        sonarRadius2 = 200;
        batX = screenWidth/2f;
        batY = screenHeight/2f;
        lives = 3; // Réinitialise les vies
        isInvincible = false; // Réinitialise l'invincibilité
    }
}
