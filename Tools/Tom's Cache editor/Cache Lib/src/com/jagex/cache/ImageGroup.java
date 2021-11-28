package com.jagex.cache;

import com.jagex.cache.util.Stream;
import com.jagex.cache.util.ExtendedByteArrayOutputStream;
import com.jagex.cache.util.Quantize;

import java.util.ArrayList;
import java.io.IOException;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author tom
 */
public final class ImageGroup {
    public int groupMaxWidth;
    public int groupMaxHeight;
    private int[] colourMap;
    private ArrayList<ImageBean> imageBeans = new ArrayList<ImageBean>();
    private int colourCount;
    private Stream indexStream;
    private Stream dataStream;
    private int indexOffset = 0;
    private int packType = 0;

    public ImageGroup() {
        groupMaxWidth = 0;
        groupMaxHeight = 0;
        colourCount = 2;
        colourMap = new int[] {0, 1};
    }

    public ImageGroup(byte[] index, byte[] data, boolean unpack) {
        indexStream = new Stream(index);
        dataStream = new Stream(data);

        indexOffset = indexStream.caret = dataStream.readUShort();
        groupMaxWidth = indexStream.readUShort();
        groupMaxHeight = indexStream.readUShort();
        colourCount = indexStream.readUByte();
        colourMap = new int[colourCount];

        for (int x = 0; x < colourCount - 1; x++) {
            colourMap[x + 1] = indexStream.readU24BitInt();
            if (colourMap[x + 1] == 0) {
                colourMap[x + 1] = 1;
            }
        }
        if (unpack) {
            unpackImages();
        }
    }

    public int countImages() {
        return imageBeans.size();
    }

    public void appendThisIndex(ExtendedByteArrayOutputStream out) throws IOException {
        indexOffset = out.size();
        out.writeShort(groupMaxWidth);
        out.writeShort(groupMaxHeight);
        out.write(colourCount);
        for (int x = 1; x < colourCount; x++) {
            out.write24Bytes(colourMap[x]);
        }
        for (ImageBean i : imageBeans) {
            out.write(i.getDrawOffsetX());
            out.write(i.getDrawOffsetY());
            out.writeShort(i.getWidth());
            out.writeShort(i.getHeight());
            out.write(packType);
        }
    }

    public byte[] packData() throws IOException {
        ExtendedByteArrayOutputStream out = new ExtendedByteArrayOutputStream();
        out.writeShort(indexOffset);
        for (ImageBean i : imageBeans) {
            int[] pixels = i.getPixels();
            if (packType == 0) {                                         //todo: Find out the advantages of each pack method
                for (int x = 0; x < pixels.length; x++) {
                    out.write(findPosInMap(pixels[x]));
                }
            } else if (packType == 1) {
                for (int x = 0; x < i.getWidth(); x++) {
                    for (int y = 0; y < i.getHeight(); y++) {
                        out.write(findPosInMap(pixels[x + y * i.getWidth()]));
                    }
                }
            }
        }
        byte[] ret = out.toByteArray();
        out.close();
        return ret;
    }

    public int findPosInMap(int colour) {
        for (int x = 0; x < colourMap.length; x++) {
            if (colour == colourMap[x]) {
                return x;
            }
        }
        return 0;
    }

    private void rebuildColourMap() {
        ArrayList<Integer> tempMap = new ArrayList<Integer>();
        tempMap.add(0); // Reserve 0 for alpha
        for (ImageBean i : imageBeans) {
            int[] pixels = i.getPixels();
            for (int x : pixels) {
                if (!tempMap.contains(x)) {
                    tempMap.add(x);
                }
            }
        }
        colourCount = tempMap.size();
        colourMap = new int[colourCount];
        for (int i = 0; i < colourCount; i++) {
            colourMap[i] = tempMap.get(i);
        }
    }

    public void unpackImages() {
        int origIndexOffset = indexStream.caret;
        int origDataOffset = dataStream.caret;
        while (dataStream.caret < dataStream.buffer.length) {
            int drawOffsetX = indexStream.readByte();
            int drawOffsetY = indexStream.readByte();
            int width = indexStream.readUShort();
            int height = indexStream.readUShort();

            packType = indexStream.readByte();
            int numPixels = width * height;

            int[] pixels = new int[numPixels];
            if (packType == 0) {                                         //todo: Find out the advantages of each pack method
                for (int x = 0; x < numPixels; x++) {
                    int i = dataStream.readUByte();
                    pixels[x] = colourMap[i];
                }
            } else if (packType == 1) {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int i = dataStream.readUByte();
                        pixels[x + y * width] = colourMap[i];
                    }
                }
            }
            imageBeans.add(new ImageBean(drawOffsetX, drawOffsetY, width, height, pixels));
        }
        indexStream.caret = origIndexOffset;
        dataStream.caret = origDataOffset;
    }

    public void replaceImage(int i, byte[] imageData, Component c) throws InterruptedException {
        ImageBean img = imageBeans.get(i);
        img.replaceFromImage(imageData, c);
        if (img.getWidth() > groupMaxWidth) groupMaxWidth = img.getWidth();
        if (img.getHeight() > groupMaxHeight) groupMaxHeight = img.getHeight();
        quantizeImage(img);
        rebuildColourMap();
    }

    public void quantizeImage(ImageBean img) {
        
        int newPix[][] = new int[img.getWidth()][img.getHeight()];
        for (int x = img.getWidth(); x-- > 0; ) {
            for (int y = img.getHeight(); y-- > 0; ) {
                newPix[x][y] = img.getPixels()[y * img.getWidth() + x];
            }
        }

        int[] newColMap = Quantize.quantizeImage(newPix, 254);
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                img.pixels[y * img.getWidth() + x] = newColMap[newPix[x][y]]; // We now have the reduced image. Time to rebuild col map
            }
        }
        for (int i = 0; i < img.pixels.length; i++) {
            int[] rgb = unpack(img.pixels[i]);
            if (rgb[0] == 255 && rgb[1] == 0 && rgb[2] == 255) {
                img.pixels[i] = 0; // Replace with alpha
            }
        }
    }

    public void addSprite(byte[] imageData, Component c) throws InterruptedException {
        ImageBean i = new ImageBean(0, 0, 1, 1, new int[]{0, 0, 0, 0}); // A dummy image
        i.replaceFromImage(imageData, c);
        if (i.getWidth() > groupMaxWidth) groupMaxWidth = i.getWidth();
        if (i.getHeight() > groupMaxHeight) groupMaxHeight = i.getHeight();
        imageBeans.add(i);
        rebuildColourMap();
    }

    public void removeSprite(int image) {
        imageBeans.remove(image);
        rebuildColourMap(); // means we can remove some un-used colors from the map
    }

    public ImageBean getImageBean(int image) {
        return imageBeans.get(image);
    }

    private int[] unpack(int rgb) {
        int[] val = new int[3];
        val[0] = (rgb >> 16) & 0xFF; // red
        val[1] = (rgb >>  8) & 0xFF; // green
        val[2] = (rgb) & 0xFF;       // blue
        return val;
    }

    public BufferedImage getImage(int indice) {
        return toImage(imageBeans.get(indice));
    }

    public BufferedImage toImage(ImageBean i) {
        BufferedImage image = new BufferedImage(i.getWidth(), i.getHeight(), BufferedImage.TYPE_INT_RGB);
        int[] pixels = i.getPixels();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = pixels[x+y*image.getWidth()];
                if (rgb == 0) {
                    rgb = 0xFF00FF;
                }
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }
}
