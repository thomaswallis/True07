package com.jagex.cache;

import java.awt.*;
import java.awt.image.PixelGrabber;

/**
 * @author tom
*/
public class ImageBean {
    private int drawOffsetX, drawOffsetY;
    private int width, height;
    public int[] pixels;

    ImageBean(int drawOffsetX, int drawOffsetY, int width, int height, int[] pixels) {
        this.drawOffsetX = drawOffsetX;
        this.drawOffsetY = drawOffsetY;
        this.width = width;
        this.height = height;
        this.pixels = pixels;
    }

    public int getDrawOffsetX() {
        return drawOffsetX;
    }

    public void setDrawOffsetX(int drawOffsetX) {
        this.drawOffsetX = drawOffsetX;
    }

    public int getDrawOffsetY() {
        return drawOffsetY;
    }

    public void setDrawOffsetY(int drawOffsetY) {
        this.drawOffsetY = drawOffsetY;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int[] getPixels() {
        return pixels;
    }

    public void setPixels(int[] pixels) {
        this.pixels = pixels;
    }

    public void replaceFromImage(byte[] imageData, Component component) throws InterruptedException {
        Image image = Toolkit.getDefaultToolkit().createImage(imageData);
        MediaTracker mediatracker = new MediaTracker(component);
        mediatracker.addImage(image, 0);
        mediatracker.waitForAll();
        setWidth(image.getWidth(component));
        setHeight(image.getHeight(component));
        pixels = new int[getWidth() * getHeight()];
        PixelGrabber pixelgrabber = new PixelGrabber(image, 0, 0, getWidth(), getHeight(), pixels, 0, getWidth());
        pixelgrabber.grabPixels();
    }
}
