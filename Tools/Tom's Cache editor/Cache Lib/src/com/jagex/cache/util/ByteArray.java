package com.jagex.cache.util;

/**
 * @author tom
 */
public class ByteArray {
    private byte[] bytes;
    public int length = 0;

    public ByteArray(byte[] bytes) {
        this.bytes = bytes;
        length = bytes.length;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
        length = bytes.length;
    }
}