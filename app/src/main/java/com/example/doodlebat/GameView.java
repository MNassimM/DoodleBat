package com.example.doodlebat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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

    private int desiredWidth = 200; // TARGET : Ajustez ces valeurs
    private int desiredHeight = 200;

    private Bitmap[] batFrames = new Bitmap[2];
    private int currentFrameIndex = 0;
    private int frameCounter = 0;
    private static final int FRAME_DELAY = 10; // Ajustez pour la vitesse

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        Bitmap original1 = BitmapFactory.decodeResource(getResources(), R.drawable.bat_frame_1);
        Bitmap original2 = BitmapFactory.decodeResource(getResources(), R.drawable.bat_frame_2);

        batFrames[0] = Bitmap.createScaledBitmap(original1, desiredWidth, desiredHeight, true);
        batFrames[1] = Bitmap.createScaledBitmap(original2, desiredWidth, desiredHeight, true);

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
            canvas.drawColor(isDark ? Color.BLACK : Color.WHITE);
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

            // Dessiner les obstacles
            paint.setColor(Color.RED);
            for (Obstacle obstacle : obstacles) {
                canvas.drawRect(obstacle.x, obstacle.y, obstacle.x + obstacle.width, obstacle.y + obstacle.height, paint);
            }
        }
    }

    public void update() {
        // Gère l'animation
        frameCounter++;
        if (frameCounter >= FRAME_DELAY) {
            currentFrameIndex = (currentFrameIndex + 1) % 2;
            frameCounter = 0;
        }
        // Générer des obstacles aléatoires
        if (random.nextInt(100) < 5) {
            int height = random.nextInt(200) + 100;
            int y = random.nextBoolean() ? 0 : (screenHeight - height);
            obstacles.add(new Obstacle(screenWidth, y, 50, height));
        }

        // Déplacer les obstacles et vérifier collision
        Iterator<Obstacle> iterator = obstacles.iterator();
        while (iterator.hasNext()) {
            Obstacle obstacle = iterator.next();
            obstacle.x -= 10;

            if (obstacle.x + obstacle.width < 0) {
                iterator.remove();
            }

            if (batX + 50 > obstacle.x && batX - 50 < obstacle.x + obstacle.width &&
                    batY + 50 > obstacle.y && batY - 50 < obstacle.y + obstacle.height) {
                // Game over
                System.exit(0);
            }
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
}
