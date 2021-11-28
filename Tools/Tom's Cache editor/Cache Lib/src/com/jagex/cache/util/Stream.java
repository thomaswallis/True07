package com.jagex.cache.util;

/**
 * @author tom
 */
public final class Stream {

    public Stream(byte abyte0[])
    {
        buffer = abyte0;
        caret = 0;
    }

    public void writeByte(int i)
    {
        buffer[caret++] = (byte)i;
    }

    public void writeShort(int i)
    {
        buffer[caret++] = (byte)(i >> 8);
        buffer[caret++] = (byte)i;
    }

    public void writeLEShort(boolean flag, int i)
    {
        buffer[caret++] = (byte)i;
        buffer[caret++] = (byte)(i >> 8);
    }

    public void write24BitInt(int i)
    {
        buffer[caret++] = (byte)(i >> 16);
        buffer[caret++] = (byte)(i >> 8);
        buffer[caret++] = (byte)i;
    }

    public void writeInt(int i)
    {
        buffer[caret++] = (byte)(i >> 24);
        buffer[caret++] = (byte)(i >> 16);
        buffer[caret++] = (byte)(i >> 8);
        buffer[caret++] = (byte)i;
    }

    public void writeLong(long l)
    {
        buffer[caret++] = (byte)(int)(l >> 56);
        buffer[caret++] = (byte)(int)(l >> 48);
        buffer[caret++] = (byte)(int)(l >> 40);
        buffer[caret++] = (byte)(int)(l >> 32);
        buffer[caret++] = (byte)(int)(l >> 24);
        buffer[caret++] = (byte)(int)(l >> 16);
        buffer[caret++] = (byte)(int)(l >> 8);
        buffer[caret++] = (byte)(int)l;
    }

    public void writeString(String s)
    {
        s.getBytes(0, s.length(), buffer, caret);
        caret += s.length();
        buffer[caret++] = 10;
    }

    public void writeBytes(byte abyte0[], int i, boolean flag, int j)
    {
        if(!flag)
            aBoolean1401 = !aBoolean1401;
        for(int k = j; k < j + i; k++)
            buffer[caret++] = abyte0[k];

    }

    public int readUByte()
    {
        return buffer[caret++] & 0xff;
    }

    public byte readByte()
    {
        return buffer[caret++];
    }

    public int readUShort()
    {
        caret += 2;
        return ((buffer[caret - 2] & 0xff) << 8) + (buffer[caret - 1] & 0xff);
    }

    public int readU24BitInt()
    {
        caret += 3;
        return ((buffer[caret - 3] & 0xff) << 16) + ((buffer[caret - 2] & 0xff) << 8) + (buffer[caret - 1] & 0xff);
    }

    public int readUInt()
    {
        caret += 4;
        return ((buffer[caret - 4] & 0xff) << 24) + ((buffer[caret - 3] & 0xff) << 16) + ((buffer[caret - 2] & 0xff) << 8) + (buffer[caret - 1] & 0xff);
    }

    public long readLong()
    {
        long l = (long) readUInt() & 0xffffffffL;
        long l1 = (long) readUInt() & 0xffffffffL;
        return (l << 32) + l1;
    }

    public String readString()
    {
        int i = caret;
        while(buffer[caret++] != 10) ;
        return new String(buffer, i, caret - i - 1);
    }

    public int readSmart2()
    {
        int i = buffer[caret] & 0xff;
        if(i < 128)
            return readUByte();
        else
            return readUShort() - 32768;
    }

    public int readSmart()
    {
        int i = buffer[caret] & 0xff;
        if(i < 128)
            return readUByte() - 64;
        else
            return readUShort() - 49152;
    }

	public void writeSmart(int i)
	{
		if(i < 63 && i > -64)
			writeByte(i + 64);
		if(i < 16384 && i >= -16384) {
			writeShort(i + 49152);
		}
	}

    private boolean aBoolean1401;
    private boolean aBoolean1403;
    public byte buffer[];
    public int caret;
    public int anInt1407;
    public static boolean aBoolean1418;
}
