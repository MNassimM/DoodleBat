package com.example.doodlebat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

    private int desiredWidth = 130; // TARGET : Ajustez ces valeurs
    private int desiredHeight = 130;

    private Bitmap[] batFrames = new Bitmap[2];
    private int currentFrameIndex = 0;
    private int frameCounter = 0;
    private static final int FRAME_DELAY = 10; // Ajustez pour la vitesse
    private Bitmap background, scaledBackground;
    private float backgroundX = 0; // Position X pour le défilement
    private int backgroundSpeed = 5; // Vitesse de défilement

    private Paint paint = new Paint();

    private boolean gameOver = false;
    private GameOverListener gameOverListener;

    private int score=0;

    public interface GameOverListener {
        void onGameOver(int finalscore);
    }

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }


    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        Bitmap original1 = BitmapFactory.decodeResource(getResources(), R.drawable.bat_frame_1);
        Bitmap original2 = BitmapFactory.decodeResource(getResources(), R.drawable.bat_frame_2);

        batFrames[0] = Bitmap.createScaledBitmap(original1, desiredWidth, desiredHeight, true);
        batFrames[1] = Bitmap.createScaledBitmap(original2, desiredWidth, desiredHeight, true);

        background = BitmapFactory.decodeResource(getResources(), R.drawable.background);


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
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread.setRunning(true);
        thread.start();
        screenWidth = getWidth();
        screenHeight = getHeight();
        scaledBackground = Bitmap.createScaledBitmap(background, screenWidth, screenHeight, true);
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
                // Dessinez un calque semi-transparent
                Paint overlayPaint = new Paint();
                overlayPaint.setColor(Color.argb(150, 0, 0, 0));
                canvas.drawRect(0, 0, screenWidth, screenHeight, overlayPaint);
            }
            if (isDark) {
                Paint darkPaint = new Paint();
                ColorMatrix colorMatrix = new ColorMatrix();
                colorMatrix.setScale(0.5f, 0.5f, 0.5f, 1.0f); // Reduce brightness by 50%
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
                darkPaint.setColorFilter(filter);
                canvas.drawBitmap(scaledBackground, backgroundX, 0, darkPaint);
                canvas.drawBitmap(scaledBackground, backgroundX + screenWidth, 0, darkPaint);
            } else {
                canvas.drawBitmap(scaledBackground, backgroundX, 0, null);
                canvas.drawBitmap(scaledBackground, backgroundX + screenWidth, 0, null);
            }

            // Défilement
            backgroundX -= backgroundSpeed;
            if (backgroundX <= -screenWidth) {
                backgroundX = 0; // Répète le cycle
            }
            Paint paint = new Paint();
            //Dessiner la bat
            Bitmap currentBitmap = batFrames[currentFrameIndex];
            if (currentBitmap != null) {
                canvas.drawBitmap(
                        currentBitmap,
                        batX - (float) currentBitmap.getWidth() / 2,
                        batY - (float) currentBitmap.getHeight() / 2,
                        null
                );
            }

            if (isDark) {
                drawSonarWaves(canvas, paint);
            } else {
                paint.setColor(Color.RED);
                for (Obstacle obstacle : obstacles) {
                    canvas.drawRect(obstacle.x, obstacle.y, obstacle.x + obstacle.width, obstacle.y + obstacle.height, paint);
                }
            }
        }
    }


    private void drawSonarWaves(Canvas canvas, Paint paint) {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        canvas.drawCircle(batX, batY, sonarRadius1, paint);
        canvas.drawCircle(batX, batY, sonarRadius2, paint);

        paint.setColor(Color.RED);
        for (Obstacle obstacle : obstacles) {
            if (isObstacleTouchedByWave(obstacle, sonarRadius1) || isObstacleTouchedByWave(obstacle, sonarRadius2)) {
                canvas.drawRect(obstacle.x, obstacle.y, obstacle.x + obstacle.width, obstacle.y + obstacle.height, paint);
            }
        }
    }

    private boolean isObstacleTouchedByWave(Obstacle obstacle, float sonarRadius) {
        return Math.hypot(obstacle.x - batX, obstacle.y - batY) <= sonarRadius ||
                Math.hypot(obstacle.x + obstacle.width - batX, obstacle.y - batY) <= sonarRadius ||
                Math.hypot(obstacle.x - batX, obstacle.y + obstacle.height - batY) <= sonarRadius ||
                Math.hypot(obstacle.x + obstacle.width - batX, obstacle.y + obstacle.height - batY) <= sonarRadius;
    }

    public void update() {
        if (gameOver) return;
        // Gère l'animation
        frameCounter++;
        if (frameCounter >= FRAME_DELAY) {
            currentFrameIndex = (currentFrameIndex + 1) % 2;
            frameCounter = 0;
        }
        // Générer des obstacles aléatoires
        if (random.nextInt(100) < 5) {
            int height = random.nextInt(500) + 200;
            int y = random.nextBoolean() ? 0 : (screenHeight - height);
            int minDistance = 200; // Minimum distance between obstacles

            if (obstacles.isEmpty() || (screenWidth - obstacles.get(obstacles.size() - 1).x) > minDistance) {
                obstacles.add(new Obstacle(screenWidth, y, 50, height));
            }
        }

        // Déplacer les obstacles et vérifier collision
        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            obstacle.x -= 5;

            if (obstacle.x + obstacle.width < 0) {
                iterator.remove();
            }

            if (batX + 50 > obstacle.x && batX - 50 < obstacle.x + obstacle.width &&
                    batY + 50 > obstacle.y && batY - 50 < obstacle.y + obstacle.height) {
                gameOver = true;
                if (gameOverListener != null) gameOverListener.onGameOver(score);
            }
        }
        long currentTime = System.currentTimeMillis();
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
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            batX -= event.values[0] * 5;
            if (batX < 0) batX = 0;
            if (batX > screenWidth) batX = screenWidth;
        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            isDark = event.values[0] < 5;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
    }
}
