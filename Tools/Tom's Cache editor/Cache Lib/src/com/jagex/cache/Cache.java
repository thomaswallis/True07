package com.jagex.cache;

import com.jagex.cache.util.ByteArray;

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * @author tom
 */
public class Cache {
    private ArrayList<CacheIndice> indices;
    private String[] indexFiles;
    private String dataFile;
    private String cacheDir;

    public Cache(String cacheDirectory) throws IOException {
        this.cacheDir = cacheDirectory;
        File[] files = new File(cacheDirectory).listFiles();
        dataFile = findDataFile(files);
        indexFiles = findIndexFiles(files);
        java.util.Arrays.sort(indexFiles);
        if (dataFile == null) {
            throw new IOException("Unable to locate cache data file!\nCorrect cache loaded?");
        }
        if (indexFiles.length == 0) {
            throw new IOException("Unable to locate cache index files!\nCorrect cache loaded?");
        }
        indices = new ArrayList<CacheIndice>();
        RandomAccessFile data = new RandomAccessFile(dataFile, "rw");
        for (int i = 0; i < indexFiles.length; i++) {
            CacheIndice indice = new CacheIndice(data, new RandomAccessFile(indexFiles[i], "rw"), i+1);
            indices.add(indice);
        }
    }

    public void rebuildCache() throws IOException {
        for (int i = 0; i < indexFiles.length; i++) {
            CacheIndice index = indices.get(i);
            ArrayList<ByteArray> files = index.getFiles();
            int currentFile = 0;
            for (ByteArray a : files) {
                int x = currentFile++;
                if (a != null) {
                    index.addOrEditFile(x, a.length, a.getBytes(), false);
                }
            }
        }
    }

    public CacheIndice getIndice(int i) {
        return indices.get(i);
    }

    public ArrayList<CacheIndice> getIndices() {
        return indices;
    }

    public String getIndexFile(int i) {
        return indexFiles[i];
    }

    public String[] getIndexFiles() {
        return indexFiles;
    }

    public String getDataFile() {
        return dataFile;
    }

    public String findDataFile(File[] files) {
        for (File file : files) {
            String s = file.getAbsolutePath();
            if (s.endsWith(".dat")) {
                if (s.contains("file_cache")) {
                    return s;
                }
            }
        }
        return null;
    }

    public String[] findIndexFiles(File[] files) {
        ArrayList<String> indices = new ArrayList<String>();
        for (File file : files) {
            String s = file.getAbsolutePath();
            if (s.contains(".idx")) {
                if (s.contains("cache")) {
                    indices.add(s);
                }
            }
        }
        String[] s = new String[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            s[i] = indices.get(i);
        }
        return s;
    }
}
