package com.example.doodlebat;

import android.graphics.Bitmap;

class Obstacle {
    public int x, y, width, height;
    public Bitmap bitmap;

    public Obstacle(int x, int y, int width, int height, Bitmap bitmap) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bitmap = Bitmap.createScaledBitmap(bitmap, width+100, height+60, true);
    }
}