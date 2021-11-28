package com.jagex.cache.configloaders;

import com.jagex.cache.util.ExtendedByteArrayOutputStream;
import com.jagex.cache.util.Stream;
import com.jagex.cache.util.exceptions.ConfigException;

import java.io.IOException;
import java.awt.*;

/**
 * @author Tom
 */
public class FloorConfig
{
    private Color mapColor = null;
    private static int floorCount;
    private static FloorConfig floorCacheConfig[];
    private String floorName = "null";
    private Color actualColor = null;
    private int texture = -1;
    private boolean unknown = false;
    private boolean occlude = true;

    public static void loadData(byte[] floorData) throws ConfigException {
        Stream stream = new Stream(floorData);
        floorCount = stream.readUShort();
        if(floorCacheConfig == null)
            floorCacheConfig = new FloorConfig[floorCount];
        for(int j = 0; j < floorCount; j++)
        {
            if(floorCacheConfig[j] == null)
                floorCacheConfig[j] = new FloorConfig();
            floorCacheConfig[j].readValues(stream);
        }
    }

    public static FloorConfig getFloor(int i) {
        return floorCacheConfig[i];
    }

    public static byte[] repack() throws IOException {
        ExtendedByteArrayOutputStream bos = new ExtendedByteArrayOutputStream();
        bos.writeShort(floorCount);
        for (FloorConfig f : floorCacheConfig) {
            if (f.unknown) {
                bos.write(3); // dummy?
            }
            if (f.actualColor != null) {
                bos.write(1);
                bos.write24Bytes(f.actualColor.getRGB());
            }
            if (f.texture != -1) {
                bos.write(2);
                bos.write(f.texture);
            }
            if (!f.occlude) {
                bos.write(5);
            }
            if (!f.floorName.equals("null")) {
                bos.write(6);
                bos.writeString(f.floorName);
            }
            if (f.mapColor != null) {
                bos.write(7);
                bos.write24Bytes(f.mapColor.getRGB());
            }
            bos.write(0);
        }
        bos.close();
        return bos.toByteArray();
    }

    public void readValues(Stream stream) throws ConfigException {
        do
        {
            int type = stream.readByte();
            if(type == 0)
                return;
            if(type == 1) // color1
            {
                actualColor = new Color(stream.readU24BitInt());
            } else
            if(type == 2) {
                texture = stream.readByte();
            }
            else
            if(type == 3) // dummy?
                unknown = true;
            else
            if(type == 5)
                occlude = false;
            else
            if(type == 6)
                floorName = stream.readString();
            else
            if(type == 7) // actualColor?
            {
                mapColor = new Color(stream.readU24BitInt());
            } else
            {
                throw new ConfigException("Unrecognised configloaders code: " + type);
            }
        } while(true);
    }

    public Color getMapColor() {
        return mapColor;
    }

    public void setMapColor(Color mapColor) {
        this.mapColor = mapColor;
    }

    public static int getFloorCount() {
        return floorCount;
    }

    public static void setFloorCount(int floorCount) {
        FloorConfig.floorCount = floorCount;
    }

    public static FloorConfig[] getFloorCache() {
        return floorCacheConfig;
    }

    public static void setFloorCache(FloorConfig[] floorCacheConfig) {
        FloorConfig.floorCacheConfig = floorCacheConfig;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }

    public Color getActualColor() {
        return actualColor;
    }

    public void setActualColor(Color actualColor) {
        this.actualColor = actualColor;
    }

    public int getTexture() {
        return texture;
    }

    public void setTexture(int texture) {
        this.texture = texture;
    }

    public boolean isUnknown() {
        return unknown;
    }

    public void setUnknown(boolean unknown) {
        this.unknown = unknown;
    }

    public boolean isOccluded() {
        return occlude;
    }

    public void setOcclude(boolean occlude) {
        this.occlude = occlude;
    }
}
