package com.jagex.cache;

import com.jagex.cache.util.ByteArray;
import com.jagex.cache.util.Stream;
import com.jagex.cache.util.ExtendedByteArrayOutputStream;
import com.jagex.cache.util.DataUtils;
import com.jagex.cache.util.bzip.BZip2Decompressor;

import java.util.ArrayList;
import java.io.IOException;

/**
 * @author tom
 */
public class Archive {
    private ArrayList<ByteArray> files = new ArrayList<ByteArray>();
    byte[] finalBuffer;
    private int totalFiles;
    private ArrayList<Integer> identifiers = new ArrayList<Integer>();
    private ArrayList<Integer> decompressedSizes = new ArrayList<Integer>();
    private ArrayList<Integer> compressedSizes = new ArrayList<Integer>();
    private ArrayList<Integer> startOffsets = new ArrayList<Integer>();
    boolean compressedAsWhole;

    public Archive(byte abyte0[]) {
        Stream stream = new Stream(abyte0);
        int decompressedSize = stream.readU24BitInt();
        int compressedSize = stream.readU24BitInt();
        if (compressedSize != decompressedSize) { // we need to decompress
            byte abyte1[] = new byte[decompressedSize];
            BZip2Decompressor.decompressBuffer(abyte1, decompressedSize, abyte0, compressedSize, 6);
            finalBuffer = abyte1;
            stream = new Stream(finalBuffer);
            compressedAsWhole = true;
        } else {
            finalBuffer = abyte0;
            compressedAsWhole = false;
        }
        totalFiles = stream.readUShort();
        int offset = stream.caret + totalFiles * 10; //file info at beginning is 10 bytes per file, and we want to start file data immediately after
        for (int l = 0; l < totalFiles; l++) {
            identifiers.add(stream.readUInt());           //  4
            decompressedSizes.add(stream.readU24BitInt());//  3
            compressedSizes.add(stream.readU24BitInt());  //+ 3
            startOffsets.add(offset);                   //____
            offset += compressedSizes.get(l);               // 10
            files.add(new ByteArray(getFileAt(l)));
        }
    }

    public byte[] recompile() throws IOException {
        byte[] compressedWhole = compileUncompressed();
        int compressedWholeDecompressedSize = compressedWhole.length;
        compressedWhole = DataUtils.bz2Compress(compressedWhole);
        int compressedWholeSize = compressedWhole.length;
        byte[] compressedIndividually = compileCompressed();
        int compressedIndividuallySize = compressedIndividually.length;
        boolean compressedAsWhole = false;
        if (compressedWholeSize < compressedIndividuallySize) {
            compressedAsWhole = true;
        } // Disable this for now, For some reason its gay on me.
        ExtendedByteArrayOutputStream finalBuf = new ExtendedByteArrayOutputStream();
        if (compressedAsWhole) {
            finalBuf.write24Bytes(compressedWholeDecompressedSize);
            finalBuf.write24Bytes(compressedWholeSize);
            finalBuf.write(compressedWhole);
        } else {
            finalBuf.write24Bytes(compressedIndividuallySize);
            finalBuf.write24Bytes(compressedIndividuallySize);
            finalBuf.write(compressedIndividually);
        }
        finalBuf.close();
        return finalBuf.toByteArray();
    }

    private byte[] compileUncompressed() throws IOException {
        byte[] fileInfoSection;
        byte[] filesSection;

        ExtendedByteArrayOutputStream fileBuf = new ExtendedByteArrayOutputStream();
        for (int i = 0; i < totalFiles; i++) {
            decompressedSizes.set(i, files.get(i).length);
            compressedSizes.set(i, files.get(i).length);
            fileBuf.write(files.get(i).getBytes());
        }
        filesSection = fileBuf.toByteArray();
        fileBuf.close();
        ExtendedByteArrayOutputStream fileInfo = new ExtendedByteArrayOutputStream();
        fileInfo.writeShort(totalFiles);
        for(int i = 0; i < totalFiles; i++) {
            fileInfo.writeInt(identifiers.get(i));
            fileInfo.write24Bytes(decompressedSizes.get(i));
            fileInfo.write24Bytes(compressedSizes.get(i));
        }
        fileInfoSection = fileInfo.toByteArray();
        fileInfo.close();
        ExtendedByteArrayOutputStream finalBuffer = new ExtendedByteArrayOutputStream();
        finalBuffer.write(fileInfoSection);
        finalBuffer.write(filesSection);
        finalBuffer.close();
        return finalBuffer.toByteArray();
    }

    private byte[] compileCompressed() throws IOException {
        byte[] fileInfoSection;
        byte[] filesSection;

        ExtendedByteArrayOutputStream fileBuf = new ExtendedByteArrayOutputStream();
        for (int i = 0; i < totalFiles; i++) {
            decompressedSizes.set(i, files.get(i).length);
            byte[] compressed = DataUtils.bz2Compress(files.get(i).getBytes());
            compressedSizes.set(i, compressed.length);
            fileBuf.write(compressed);
        }
        filesSection = fileBuf.toByteArray();
        fileBuf.close();
        ExtendedByteArrayOutputStream fileInfo = new ExtendedByteArrayOutputStream();
        fileInfo.writeShort(totalFiles);
        for(int i = 0; i < totalFiles; i++) {
            fileInfo.writeInt(identifiers.get(i));
            fileInfo.write24Bytes(decompressedSizes.get(i));
            fileInfo.write24Bytes(compressedSizes.get(i));
        }
        fileInfoSection = fileInfo.toByteArray();
        fileInfo.close();
        ExtendedByteArrayOutputStream finalBuffer = new ExtendedByteArrayOutputStream();
        finalBuffer.write(fileInfoSection);
        finalBuffer.write(filesSection);
        finalBuffer.close();
        return finalBuffer.toByteArray();
    }

    public byte[] getFileAt(int at) {
        byte dataBuffer[] = new byte[decompressedSizes.get(at)];
        if (!compressedAsWhole) {
            BZip2Decompressor.decompressBuffer(dataBuffer, decompressedSizes.get(at), finalBuffer, compressedSizes.get(at), startOffsets.get(at));
        } else {
            System.arraycopy(finalBuffer, startOffsets.get(at), dataBuffer, 0, decompressedSizes.get(at));
        }
        return dataBuffer;
    }

    public byte[] getFile(int identifier) {
        for (int k = 0; k < totalFiles; k++)
            if (identifiers.get(k) == identifier) {
                return getFileAt(k);
            }
        return null;
    }

    public int getIdentifierAt(int at) {
        return identifiers.get(at);
    }

    public int getDecompressedSize(int at) {
        return decompressedSizes.get(at);
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public byte[] getFile(String identStr) {
        int identifier = 0;
        identStr = identStr.toUpperCase();
        for (int j = 0; j < identStr.length(); j++)
            identifier = (identifier * 61 + identStr.charAt(j)) - 32;
        return getFile(identifier);
    }

    public static int getHash(String s) {
        int identifier = 0;
        s = s.toUpperCase();
        for (int j = 0; j < s.length(); j++)
            identifier = (identifier * 61 + s.charAt(j)) - 32;
        return identifier;
    }

    public void renameFile(int index, int newName) {
        identifiers.set(index, newName);
    }

    public void updateFile(int index, byte[] data) {
        files.get(index).setBytes(data);
    }

    public int indexOf(String name) {
        return indexOf(getHash(name));
    }

    public int indexOf(int hash) {
        return identifiers.indexOf(hash);
    }

    public void removeFile(int index) {
        files.remove(index);
        identifiers.remove(index);
        compressedSizes.remove(index);
        decompressedSizes.remove(index);
        totalFiles--;
    }

    public void addFile(int identifier, byte[] data) {
        identifiers.add(identifier);
        decompressedSizes.add(data.length);
        compressedSizes.add(0);
        files.add(new ByteArray(data));
        totalFiles++;
    }

    public void addFileAt(int at, int identifier, byte[] data) {
        identifiers.add(at, identifier);
        decompressedSizes.add(at, data.length);
        compressedSizes.add(at, 0);
        files.add(at, new ByteArray(data));
        totalFiles++;
    }
}